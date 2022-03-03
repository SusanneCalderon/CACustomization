/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.adempiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.wstore.ExpenseServlet;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.X_HR_Concept;

//import com.Verisign.payment.f;

/**
 *	Create AP Invoices from Expense Reports
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ExpenseAPInvoice.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class SB_ExpenseAPInvoice extends SB_ExpenseAPInvoiceAbstract
{
	private HashMap <Integer, MInvoice> invoices  = new HashMap<>();
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		super.prepare();
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws java.lang.Exception
	{
		int old_BPartner_ID = 0;
		MBPartner partner = null;
		MInvoice invoice = null;
		for(Integer key : getSelectionKeys()) {	

			MTimeExpense timeExpense = new MTimeExpense (getCtx(), key, get_TrxName());
			for (MTimeExpenseLine expenseLine:timeExpense.getLines()) {
				if (expenseLine.getC_InvoiceLine_ID() != 0
						|| Env.ZERO.compareTo(expenseLine.getQtyReimbursed()) == 0
						|| Env.ZERO.compareTo(expenseLine.getPriceReimbursed()) == 0)
					continue;

				if (expenseLine.getC_BPartner_ID() != old_BPartner_ID) {
					partner = new MBPartner (getCtx(), expenseLine.getC_BPartner_ID(), get_TrxName());
					if (!invoices.containsKey(expenseLine.getC_BPartner_ID()))
					{
						invoice = new MInvoice (getCtx(), 0, null);
						invoice.setClientOrg(expenseLine.getAD_Client_ID(), expenseLine.getAD_Org_ID());
						invoice.setBPartner(partner);
						invoice.setC_DocTypeTarget_ID(getDocTypeId());	//	API
						invoice.setM_PriceList_ID(expenseLine.getS_TimeExpense().getM_PriceList_ID());
						invoice.setSalesRep_ID(expenseLine.getS_TimeExpense().getCreatedBy());
						String descr = Msg.translate(getCtx(), "S_TimeExpense_ID") 
								+ ": " + expenseLine.getS_TimeExpense().getDocumentNo() + " " 
								+ DisplayType.getDateFormat(DisplayType.Date).format(expenseLine.getS_TimeExpense().getDateReport());  
						invoice.setDescription(descr);
						invoice.saveEx();
						invoices.put(invoice.getC_BPartner_ID(), invoice);
						old_BPartner_ID = partner.getC_BPartner_ID();
					}
					else {
						invoice = invoices.get(partner.getC_BPartner_ID());
						old_BPartner_ID = partner.getC_BPartner_ID();
					}
					//	Create OrderLine
					MInvoiceLine invoiceLine = new MInvoiceLine (invoice);
					//
					if (expenseLine.getM_Product_ID() != 0)
						invoiceLine.setM_Product_ID(expenseLine.getM_Product_ID(), true);
					invoiceLine.setQty(expenseLine.getQtyReimbursed());		//	Entered/Invoiced
					invoiceLine.setDescription(expenseLine.getDescription());
					//
					invoiceLine.setC_Project_ID(expenseLine.getC_Project_ID());
					invoiceLine.setC_ProjectPhase_ID(expenseLine.getC_ProjectPhase_ID());
					invoiceLine.setC_ProjectTask_ID(expenseLine.getC_ProjectTask_ID());
					invoiceLine.setC_Activity_ID(expenseLine.getC_Activity_ID());
					invoiceLine.setC_Campaign_ID(expenseLine.getC_Campaign_ID());
					//
					//	il.setPrice();	//	not really a list/limit price for reimbursements
					invoiceLine.setPrice(expenseLine.getPriceReimbursed());	//
					invoiceLine.setTax();
					X_HR_Concept concept = new X_HR_Concept(getCtx(), expenseLine.get_ValueAsInt(X_HR_Concept.COLUMNNAME_HR_Concept_ID),get_TrxName());
					String description = invoiceLine.getC_Project().getName() + " "  
							+ DisplayType.getDateFormat(DisplayType.Date).format(expenseLine.getDateExpense()) +  " " + concept.getName();
					BigDecimal feeAmt = (BigDecimal)expenseLine.get_Value("FeeAmt");
					BigDecimal travelCost =  (BigDecimal)expenseLine.get_Value("TravelCost");
					if (feeAmt.signum()>0)
						description = description + " "   + Msg.translate(getCtx(), "ExtraFee ") + feeAmt.toString();
					if (travelCost.signum()>0)
						description = description + " "   + Msg.translate(getCtx(), "TravelCost ") + travelCost.toString();
					invoiceLine.setDescription(description);
					invoiceLine.saveEx();
					//	Update TEL
					expenseLine.setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
					expenseLine.saveEx();
				}
			}
		}
		return "@Created@=" + invoices.size();
	}	//	doIt

	/**
	 * 	Complete Invoice
	 *	@param invoice invoice
	 */
	private void completeInvoice (MInvoice invoice)
	{
		if (invoice == null)
			return;
		invoice.setDocAction(DocAction.ACTION_Prepare);
		invoice.processIt(DocAction.ACTION_Prepare);
		if (!invoice.save())
			new IllegalStateException("Cannot save Invoice");
		//
		addLog(invoice.get_ID(), invoice.getDateInvoiced(), 
			invoice.getGrandTotal(), invoice.getDocumentNo());
	}	//	completeInvoice

}	//	ExpenseAPInvoice

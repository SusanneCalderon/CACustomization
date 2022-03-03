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
package org.compiere.process;


import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionAmt;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 *	Create AP Invoices for Commission
 *	
 *  @author Jorg Janke
 *  @version $Id: CommissionAPInvoice.java,v 1.2 2006/07/30 00:51:01 jjanke Exp $
 */
public class CommissionAPInvoice extends SvrProcess
{
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (variables are parsed)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		log.info("doIt - C_CommissionRun_ID=" + getRecord_ID());
		//	Load Data
		MCommissionRun comRun = new MCommissionRun (getCtx(), getRecord_ID(), get_TrxName());
		if (comRun.get_ID() == 0)
			throw new IllegalArgumentException("CommissionAPInvoice - No Commission Run");
		if (Env.ZERO.compareTo(comRun.getGrandTotal()) == 0)
			throw new IllegalArgumentException("@GrandTotal@ = 0");
		MCommission com = new MCommission (getCtx(), comRun.getC_Commission_ID(), get_TrxName());
		if (com.get_ID() == 0)
			throw new IllegalArgumentException("CommissionAPInvoice - No Commission");
		if (com.getC_Charge_ID() == 0)
			throw new IllegalArgumentException("CommissionAPInvoice - No Charge on Commission");
		for (MBPartner salesRep: com.getSalesRepsOfCommission()) {
			
		
		//	Create Invoice
			if(getInvoiceofBpartnerCommission(comRun.getC_CommissionRun_ID(), salesRep.getC_BPartner_ID()).signum() ==1) {
				return "Ya facturado";
			}
		MInvoice invoice = new MInvoice (getCtx(), 0, null);
		invoice.setClientOrg(com.getAD_Client_ID(), com.getAD_Org_ID());
		invoice.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_APInvoice);	//	API
		invoice.setC_Currency_ID(com.getC_Currency_ID());
		invoice.setBPartner(salesRep);
		invoice.set_ValueOfColumn(MCommissionRun.COLUMNNAME_C_CommissionRun_ID, comRun.getC_CommissionRun_ID());
	//	invoice.setDocumentNo (comRun.getDocumentNo());		//	may cause unique constraint
		invoice.setSalesRep_ID(getAD_User_ID());	//	caller
		//
		if (com.getC_Currency_ID() != invoice.getC_Currency_ID())
			throw new IllegalArgumentException("CommissionAPInvoice - Currency of PO Price List not Commission Currency");
		//		
		if (!invoice.save())
			throw new IllegalStateException("CommissionAPInvoice - cannot save Invoice");
			
 		//	Create Invoice Line
 		MInvoiceLine iLine = new MInvoiceLine(invoice);
 		BigDecimal totalAmt = getTotalofBpartner(comRun.getC_CommissionRun_ID(), salesRep.getC_BPartner_ID());
 		if (totalAmt.signum()==0) {
 			invoice.delete(true);
 			continue;
 		}
		iLine.setC_Charge_ID(com.getC_Charge_ID());
 		iLine.setQty(1);
 		iLine.setPrice(totalAmt);
		iLine.setTax();
		if (!iLine.save())
			throw new IllegalStateException("CommissionAPInvoice - cannot save Invoice Line");
		}
		//
		return "";
	}	//	doIt
	
	private BigDecimal getTotalofBpartner(int C_CommissionRun_ID, int C_Bpartner_ID) {
		BigDecimal totalCommission = new Query(getCtx(), MCommissionAmt.Table_Name, "C_CommissionRun_ID=? and C_Bpartner_ID=?", get_TrxName())
				.setParameters(C_CommissionRun_ID, C_Bpartner_ID)
				.aggregate(MCommissionAmt.COLUMNNAME_CommissionAmt, Query.AGGREGATE_SUM);
		
		return totalCommission;
	}

	private BigDecimal getInvoiceofBpartnerCommission(int C_CommissionRun_ID, int C_Bpartner_ID) {
		BigDecimal countInvoices = new Query(getCtx(), MInvoice.Table_Name, "C_CommissionRun_ID=? and C_Bpartner_ID=? And Docstatus not in('VO','RE')", get_TrxName())
				.setParameters(C_CommissionRun_ID, C_Bpartner_ID)
				.aggregate(MInvoice.COLUMNNAME_C_Invoice_ID, Query.AGGREGATE_COUNT);
		
		return countInvoices;
	}

}	//	CommissionAPInvoice

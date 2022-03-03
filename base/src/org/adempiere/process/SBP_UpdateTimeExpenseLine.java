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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MConversionRate;
import org.compiere.model.MInvoice;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.eevolution.model.MHRAttribute;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.MHREmployee;

/**
 *	Create AP Invoices from Expense Reports
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ExpenseAPInvoice.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class SBP_UpdateTimeExpenseLine extends SvrProcess
{
	private HashMap <Integer, MInvoice> invoices  = new HashMap<>();
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws java.lang.Exception
	{
		
		Properties A_Ctx = getCtx();
		String A_TrxName = get_TrxName();
		int A_AD_PInstance_ID = getAD_PInstance_ID();

	        List<MTimeExpenseLine> m_records = null;
	        
	        String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE  T_Selection.AD_PInstance_ID=? " +
	                " AND T_Selection.T_Selection_ID=S_TimeExpenseLine.S_TimeExpenseLine_ID)";
	        m_records = new Query(A_Ctx, MTimeExpenseLine.Table_Name, whereClause, A_TrxName)
	                                            .setParameters(A_AD_PInstance_ID)
	                                            .setClient_ID()
	                                            .list();
	        for (MTimeExpenseLine expenseLine:m_records){
	            MHRConcept conceptProduct = new MHRConcept(expenseLine.getCtx(), expenseLine.get_ValueAsInt(MHRConcept.COLUMNNAME_HR_Concept_ID), expenseLine.get_TrxName());
	            MHRAttribute productattribute = MHRAttribute.getByConceptAndEmployee(conceptProduct, null, 0, expenseLine.getParent().getDateReport(), expenseLine.getParent().getDateReport());
	            BigDecimal factor = productattribute.getAmount();
	        
	            MHRConcept salaryConcept = MHRConcept.getByValue(expenseLine.getCtx(), "Salarios", expenseLine.get_TrxName());
	          
	            MHRAttribute salaryAttribute = MHRAttribute.getByConceptIdAndPartnerId(A_Ctx, salaryConcept.getHR_Concept_ID(), 
	            		expenseLine.getC_BPartner_ID(), expenseLine.getParent().getDateReport(), A_TrxName);
	            BigDecimal salaryhour = salaryAttribute !=null? salaryAttribute.getAmount().divide(new BigDecimal(30.00), 2, BigDecimal.ROUND_HALF_UP):Env.ZERO;
	        
	            salaryhour = salaryhour.divide(new BigDecimal(8.00), 2, BigDecimal.ROUND_HALF_UP);
	            salaryhour = salaryhour.multiply(factor);
	            BigDecimal costtotal = salaryhour.multiply(expenseLine.getQty());
	            expenseLine.setExpenseAmt(salaryhour);

	            if (expenseLine.getC_Currency_ID() == expenseLine.getC_Currency_Report_ID())
	                expenseLine.setConvertedAmt(costtotal);
	            else
	            {
	                expenseLine.setConvertedAmt(MConversionRate.convert (expenseLine.getCtx(),
	                    costtotal, expenseLine.getC_Currency_ID(), expenseLine.getC_Currency_Report_ID(), 
	                    expenseLine.getDateExpense(), 0, getAD_Client_ID(), expenseLine.getAD_Org_ID()) );
	            }
	            BigDecimal feeAmt = (BigDecimal)expenseLine.get_Value("FeeAmt");
	            BigDecimal travelCost =  (BigDecimal)expenseLine.get_Value("TravelCost");
	            BigDecimal prepaymentAmt =  (BigDecimal)expenseLine.get_Value("PrepaymentAmt");
	            if (conceptProduct.isInvoiced()) {
	                expenseLine.setQtyReimbursed(expenseLine.getQty());
	                expenseLine.setPriceReimbursed(expenseLine.getExpenseAmt().add(feeAmt).add(travelCost).subtract(prepaymentAmt));
	                expenseLine.saveEx();
	            }
	            else
	            {
	                expenseLine.setQtyReimbursed(Env.ONE);
	                expenseLine.setPriceReimbursed(feeAmt.add(travelCost).subtract(prepaymentAmt));
	                expenseLine.saveEx();
	            }
	        }
		return "";
	}	//	doIt


}	//	ExpenseAPInvoice

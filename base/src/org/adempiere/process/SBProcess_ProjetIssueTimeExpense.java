/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.adempiere.process;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Msg;

/** Generated Process for (SBProcess_ProjetIssueTimeExpense)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class SBProcess_ProjetIssueTimeExpense extends SBProcess_ProjetIssueTimeExpenseAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		//	Sum all
		for(int timeExpenseLineID : getSelectionKeys()) {
			BigDecimal movementQty = getSelectionAsBigDecimal(timeExpenseLineID, "PI_QtyToDeliver");
			MTimeExpenseLine timeExpenseLine = new MTimeExpenseLine(getCtx(), timeExpenseLineID, get_TrxName());
			if (timeExpenseLine.getM_Product_ID() == 0 || timeExpenseLine.getQty().signum() == 0)
				continue;
			createLine(timeExpenseLine, movementQty);
			}
		return "";
	}

	private void createLine(MTimeExpenseLine timeExpenseLine, BigDecimal movementQty) {
		//	Create Issue
		if (timeExpenseLine.getC_Project_ID() == 0)
			return;
		MProject project = (MProject)timeExpenseLine.getC_Project();
		MTimeExpense timeExpense = (MTimeExpense)timeExpenseLine.getS_TimeExpense();
		MProjectIssue projectIssue = null;
		projectIssue = new Query(getCtx(), MProjectIssue.Table_Name, "S_TimeExpenseLine_ID=?", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(timeExpenseLine.getS_TimeExpenseLine_ID())
				.first();
		if (projectIssue == null) {
			projectIssue = new MProjectIssue(project);
			projectIssue.setMandatory(timeExpense.getM_Locator_ID(), timeExpenseLine.getM_Product_ID(), timeExpenseLine.getQty());
			if (getMovementDate() != null)        //	default today
				projectIssue.setMovementDate(getMovementDate());
			if (getDescription() != null && getDescription().length() > 0)
				projectIssue.setDescription(getDescription());
			else if (timeExpenseLine.getDescription() != null)
				projectIssue.setDescription(timeExpenseLine.getDescription());
			else if (timeExpenseLine.getS_TimeExpense().getDescription() != null)
				projectIssue.setDescription(timeExpenseLine.getS_TimeExpense().getDescription());
			projectIssue.setS_TimeExpenseLine_ID(timeExpenseLine.getS_TimeExpenseLine_ID());
			if (timeExpenseLine.getC_ProjectPhase_ID() != 0)
				projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectPhase_ID, timeExpenseLine.getC_ProjectPhase_ID());
			if (timeExpenseLine.getC_ProjectTask_ID()!=0)
				projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectTask_ID, timeExpenseLine.getC_ProjectTask_ID());
			projectIssue.saveEx();
			projectIssue.process();

			//	Find/Create Project Line
			//	Find/Create Project Line
			MProjectLine projectLine = new MProjectLine(project);
			projectLine.setMProjectIssue(projectIssue);		//	setIssue
			projectLine.setCommittedAmt(projectIssue.getMovementQty().multiply(timeExpenseLine.getConvertedAmt()));
			projectLine.saveEx();
			addLog(projectIssue.getLine(), projectIssue.getMovementDate(), projectIssue.getMovementQty(), null);
		}
		if (projectIssue.getS_TimeExpenseLine().isInvoiced()&& projectIssue.getS_TimeExpenseLine().getC_InvoiceLine_ID() <=0)
			createUpdateInvoiceOfBPartner(projectIssue);
		//return "@Created@ " + counter.get();		
	}
	
	private void createUpdateInvoiceOfBPartner(MProjectIssue projectIssue){
		
		String whereClause = "C_BPartner_ID=? and docstatus in ('DR','IP') and issotrx='N'";
		MTimeExpenseLine expenseLine = (MTimeExpenseLine)projectIssue.getS_TimeExpenseLine();
		MInvoice invoice = new org.compiere.model.Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
				.setParameters(projectIssue.getS_TimeExpenseLine().getC_BPartner_ID())
				.setOnlyActiveRecords(true)
				.first();
		if (invoice == null) {
			MBPartner partner = (MBPartner)projectIssue.getS_TimeExpenseLine().getC_BPartner();
			invoice = new MInvoice(getCtx(), 0, get_TrxName());
			invoice.setClientOrg(projectIssue.getAD_Client_ID(), projectIssue.getAD_Org_ID());
			if (partner.get_ValueAsInt("C_DocTypeInvoice_ID") > 0)
				invoice.setC_DocTypeTarget_ID(partner.get_ValueAsInt("C_DocTypeInvoice_ID"));
			else 
				invoice.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_APInvoice);
			if (partner.getPO_PriceList_ID()>0)
				invoice.setM_PriceList_ID(partner.getPO_PriceList_ID());
			else
				invoice.setM_PriceList_ID(expenseLine.getS_TimeExpense().getM_PriceList_ID());
			invoice.setDateInvoiced(projectIssue.getMovementDate());
			invoice.setBPartner(partner);
			if (invoice.getC_BPartner_Location_ID() == 0)
			{
				log.log(Level.SEVERE, "No BP Location: " + partner);
				invoice = null;
				return;
			}

			invoice.setSalesRep_ID(expenseLine.getCreatedBy());
			
			String descr = Msg.translate(getCtx(), "S_TimeExpense_ID") 
				+ ": " + projectIssue.getS_TimeExpenseLine().getS_TimeExpense().getDocumentNo() + " " 
				+ DisplayType.getDateFormat(DisplayType.Date).format(expenseLine.getS_TimeExpense().getDateReport());  
			invoice.setDescription(descr);
			invoice.saveEx();		
		}
		MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
		invoiceLine.setM_Product_ID(projectIssue.getM_Product_ID());
		invoiceLine.setQty(projectIssue.getMovementQty());
		invoiceLine.setPrice(projectIssue.getS_TimeExpenseLine().getConvertedAmt());
		invoiceLine.setC_Project_ID(expenseLine.getC_Project_ID());
		invoiceLine.setC_ProjectPhase_ID(expenseLine.getC_ProjectPhase_ID());
		invoiceLine.setC_ProjectTask_ID(expenseLine.getC_ProjectTask_ID());
		invoiceLine.setC_Activity_ID(expenseLine.getC_Activity_ID());
		invoiceLine.setC_Campaign_ID(expenseLine.getC_Campaign_ID());
		invoiceLine.setTax();
		invoiceLine.saveEx();
		expenseLine.setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
		expenseLine.saveEx();		
	}
}

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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;

/** Generated Process for (SBP_ProjectIssueCreateFrom)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class SBP_ProjectIssueCreateFrom extends SBP_ProjectIssueCreateFromAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		List<Integer> recordIds =  getSelectionKeys();
		String result = "";
		String createFromType = recordIds.size() > 0 ?  getSelectionAsString(recordIds.get(0), "L_CreateFromType") : null;
		if (createFromType.equals("IO"))
			result = issueReceipt(recordIds);
		if (createFromType.equals("EX"))
			result = issueExpense(recordIds);
		return result;
	}
	
	private String issueExpense(List<Integer> recordIds)
	{
		int firstExpenseLineID= getSelectionAsInt(recordIds.get(0), "L_rv_ProjectIssueCreateFrom_ID");
		MProject project = new MProject(getCtx(), getProjectId(), get_TrxName());
		MTimeExpense expense = new MTimeExpense(getCtx(), firstExpenseLineID, get_TrxName());

		AtomicInteger counter = new AtomicInteger(0);
		recordIds.stream().forEach( key -> {
			int expenseLine_ID = getSelectionAsInt(key, "L_rv_ProjectIssueCreateFrom_ID");
			MTimeExpenseLine expenseLine = new MTimeExpenseLine(getCtx(), expenseLine_ID, get_TrxName());
			MProjectIssue projectIssue = new MProjectIssue (project);
			projectIssue.setMandatory (expense.getM_Locator_ID(), expenseLine.getM_Product_ID(), expenseLine.getQty());
			if (getMovementDate() != null)		//	default today
				projectIssue.setMovementDate(getMovementDate());
			if (getDescription() != null && getDescription().length() > 0)
				projectIssue.setDescription(getDescription());
			else if (expenseLine.getDescription() != null)
				projectIssue.setDescription(expenseLine.getDescription());
			projectIssue.setS_TimeExpenseLine_ID(expenseLine.getS_TimeExpenseLine_ID());
			projectIssue.completeIt();
			//	Find/Create Project Line
			MProjectLine projectLine = new MProjectLine(project);
			projectLine.setMProjectIssue(projectIssue);		//	setIssue
			projectLine.saveEx();
			addLog(projectIssue.getLine(), projectIssue.getMovementDate(), projectIssue.getMovementQty(), null);
			counter.getAndUpdate(no -> no + 1);
			
		});
		return "@Created@ " + counter.get();
	}	//	issueExpense
	

	private String issueReceipt(List<Integer> recordIds)
	{
		int firstInoutLineID= getSelectionAsInt(recordIds.get(0), "L_M_InOut_ID");
		MProject project = new MProject(getCtx(), getProjectId(), get_TrxName());
		MInOut inOut = new MInOut(getCtx(), firstInoutLineID, get_TrxName());

		AtomicInteger counter = new AtomicInteger(0);
		recordIds.stream().forEach( key -> {
			int inoutLineID = getSelectionAsInt(key, "L_rv_ProjectIssueCreateFrom_ID");
			MInOutLine inOutLine =  new MInOutLine(getCtx(), inoutLineID, get_TrxName());
			//	Create Issue
			MProjectIssue projectIssue = new MProjectIssue(project);
			projectIssue.setMandatory(inOutLine.getM_Locator_ID(), inOutLine.getM_Product_ID(), inOutLine.getMovementQty());
			if (getMovementDate() != null)        //	default today
				projectIssue.setMovementDate(getMovementDate());
			if (getDescription() != null && getDescription().length() > 0)
				projectIssue.setDescription(getDescription());
			else if (inOutLine.getDescription() != null)
				projectIssue.setDescription(inOutLine.getDescription());
			else if (inOut.getDescription() != null)
				projectIssue.setDescription(inOut.getDescription());
			projectIssue.setM_InOutLine_ID(inOutLine.getM_InOutLine_ID());
			projectIssue.completeIt();

			//	Find/Create Project Line
			MProjectLine firstProjectLine = null;
			for (MProjectLine projectLine: project.getLines()) {
				if (projectLine.getC_OrderPO_ID() == inOut.getC_Order_ID()
						&& projectLine.getM_Product_ID() == inOutLine.getM_Product_ID()
						&& projectLine.getC_ProjectIssue_ID() == 0) {
					firstProjectLine = projectLine;
					break;}
			}
			if (firstProjectLine == null)
				firstProjectLine = new MProjectLine(project);
			firstProjectLine.setMProjectIssue(projectIssue);        //	setIssue
			firstProjectLine.saveEx();
			addLog(projectIssue.getLine(), projectIssue.getMovementDate(), projectIssue.getMovementQty(), null);
			counter.getAndUpdate(no -> no + 1);	
			
		});
		return "@Created@ " + counter.get();
	}	//	issueExpense
}
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

import org.compiere.model.MCostDetail;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.Query;

/** Generated Process for (SBProcess_ProjectIssueFromInventory)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class SBProcess_ProjectIssueFromInventory extends SBProcess_ProjectIssueFromInventoryAbstract
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
		MProject project = new MProject(getCtx(), getProjectId(), get_TrxName());
		for(int key_ID : getSelectionKeys()) {
			BigDecimal movementQty = getSelectionAsBigDecimal(key_ID, "T_MovementQty");
			int M_Locator_ID=getSelectionAsInt(key_ID, "T_M_Locator_ID");
			int M_Product_ID=getSelectionAsInt(key_ID, "T_M_Product_ID");
			issueInventory(project,M_Product_ID, M_Locator_ID, movementQty);
			}
		return "";
	}
	

	private String issueInventory(MProject project, int M_Product_ID, int M_Locator_ID, BigDecimal movementQty)
	{
		if (M_Locator_ID == 0)
			throw new IllegalArgumentException("No Locator");
		if (M_Product_ID == 0)
			throw new IllegalArgumentException("No Product");
		//
		MProjectIssue projectIssue = new MProjectIssue (project);
		projectIssue.setMandatory (M_Locator_ID, M_Product_ID, movementQty);
		if (getProjectPhaseId() > 0)
			projectIssue.set_ValueOfColumn("C_ProjectPhase_ID", getProjectPhaseId());
		if (getProjectTaskId() > 0)
			projectIssue.set_ValueOfColumn("C_ProjectTask_ID", getProjectTaskId());
		if (getMovementDate() != null)		//	default today
			projectIssue.setMovementDate(getMovementDate());
		if (getDescription() != null && getDescription().length() > 0)
			projectIssue.setDescription(getDescription());
		projectIssue.saveEx();
		projectIssue.processIt(getDocAction());
		
		if (getDocAction().equals(MProjectIssue.DOCACTION_Complete)) {
			
		
		//	Create Project Line
		MProjectLine projectLine = new MProjectLine(project);
		projectLine.setMProjectIssue(projectIssue);
		projectLine.setC_ProjectPhase_ID(projectIssue.get_ValueAsInt(MInOutLine.COLUMNNAME_C_ProjectPhase_ID));
		projectLine.setC_ProjectTask_ID(projectIssue.get_ValueAsInt(MInOutLine.COLUMNNAME_C_ProjectTask_ID));
		MCostDetail costDetail = new Query(getCtx(), MCostDetail.Table_Name, "C_ProjectIssue_ID=?", get_TrxName())
				.setParameters(projectIssue.getC_ProjectIssue_ID())
				.first();
		if (costDetail != null) {
			projectLine.setCommittedAmt(costDetail.getCostAmt()
					.add(costDetail.getCostAmtLL())
					.add(costDetail.getCostAdjustment())
					.add(costDetail.getCostAdjustmentLL()));
		}
		projectLine.saveEx();
		addLog(projectIssue.getLine(), projectIssue.getMovementDate(), projectIssue.getMovementQty(), null);
		return "@Created@ 1";
		}

		return "@Created@ 1";

	}	//	issueInventory

}
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
import java.util.ArrayList;
import java.util.Optional;

import org.compiere.model.MCostDetail;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.MTransaction;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/** Generated Process for (SBProcess_ProjetIssueMInOut)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class SBProcess_ProjetIssueMInOut extends SBProcess_ProjetIssueMInOutAbstract
{
	StringBuffer resultmsg = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		//	Sum all
		resultmsg = new StringBuffer();
		for(int inOutLine_ID : getSelectionKeys()) {
			BigDecimal movementQty = getSelectionAsBigDecimal(inOutLine_ID, "PI_QtyToDeliver");
			MInOutLine inOutLine = new MInOutLine(getCtx(), inOutLine_ID, get_TrxName());
			if (inOutLine.getM_Product_ID() == 0 || inOutLine.getMovementQty().signum() == 0)
				continue;
			resultmsg.append(createLine(inOutLine, movementQty) + " ");
			}
		return resultmsg.toString();
	}
	
	private String createLine(MInOutLine inOutLine, BigDecimal movementQty) {
		


		//	Create Issue
		if (inOutLine.getC_Project_ID() == 0)
			return "";
		String error = checkStock(inOutLine);
		if (error.length()>0) {
			return error;
		}
		MProject project = (MProject)inOutLine.getC_Project();
		MProjectIssue projectIssue = new MProjectIssue(project);
		projectIssue.setMandatory(inOutLine.getM_Locator_ID(), inOutLine.getM_Product_ID(), movementQty);
		if (getMovementDate() != null)        //	default today
			projectIssue.setMovementDate(getMovementDate());
		if (getDescription() != null && getDescription().length() > 0)
			projectIssue.setDescription(getDescription());
		else if (inOutLine.getDescription() != null)
			projectIssue.setDescription(inOutLine.getDescription());
		else if (inOutLine.getM_InOut().getDescription() != null)
			projectIssue.setDescription(inOutLine.getM_InOut().getDescription());
		projectIssue.setM_InOutLine_ID(inOutLine.getM_InOutLine_ID());
		if (inOutLine.getC_ProjectPhase_ID() != 0)
			projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectPhase_ID, inOutLine.getC_ProjectPhase_ID());
		if (inOutLine.getC_ProjectTask_ID()!=0)
			projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectTask_ID, inOutLine.getC_ProjectTask_ID());
		projectIssue.setM_AttributeSetInstance_ID(inOutLine.getM_AttributeSetInstance_ID());
		projectIssue.saveEx();
		projectIssue.completeIt();

		//	Find/Create Project Line
		MProjectLine firstProjectLine = null;
		Optional<MProjectLine> newfirstProjectLine = null;
		if (!project.getLines().isEmpty()) {
			newfirstProjectLine = project.getLines().stream()
					.filter(projectLine -> projectLine.getC_OrderPO_ID() == inOutLine.getM_InOut().getC_Order_ID()
					&& projectLine.getM_Product_ID() == inOutLine.getM_Product_ID()
					&& projectLine.getC_ProjectIssue_ID() == 0)
					.findFirst();
			if (!newfirstProjectLine.isPresent()) {
				firstProjectLine = new MProjectLine(project);
			}
			else firstProjectLine = newfirstProjectLine.get();
		}
		if (firstProjectLine == null)
			firstProjectLine = new MProjectLine(project);
		firstProjectLine.setMProjectIssue(projectIssue);        //	setIssue
		firstProjectLine.setC_ProjectPhase_ID(projectIssue.get_ValueAsInt(MInOutLine.COLUMNNAME_C_ProjectPhase_ID));
		firstProjectLine.setC_ProjectTask_ID(projectIssue.get_ValueAsInt(MInOutLine.COLUMNNAME_C_ProjectTask_ID));
		firstProjectLine.set_ValueOfColumn(MProjectIssue.COLUMNNAME_M_InOutLine_ID, projectIssue.getM_InOutLine_ID());
		MCostDetail costDetail = new Query(getCtx(), MCostDetail.Table_Name, "C_ProjectIssue_ID=?", get_TrxName())
				.setParameters(projectIssue.getC_ProjectIssue_ID())
				.first();
		if (costDetail != null) {
			firstProjectLine.setCommittedAmt(costDetail.getCostAmt()
					.add(costDetail.getCostAmtLL())
					.add(costDetail.getCostAdjustment())
					.add(costDetail.getCostAdjustmentLL()));
		}
		firstProjectLine.saveEx();
		addLog(projectIssue.getLine(), projectIssue.getMovementDate(), projectIssue.getMovementQty(), null);
		return projectIssue.getDocumentNo();		
	}

	/**
	 * 	Check if Project Issue already has Receipt
	 * 	@param project Project
	 *	@param inOutLineId line
	 *	@return true if exists
	 */
	private boolean projectIssueHasReceipt (MProject project , int inOutLineId)
	{/*
		if (projectIssues == null)
			projectIssues = project.getIssues();
		Boolean exists = projectIssues.stream().allMatch(projectIssue -> projectIssue.getM_InOutLine_ID() == inOutLineId);
		if (exists)
			return true;
		else
			return false;*/
		return true;
	}	//	projectIssueHasReceipt
	
	private String checkStock(MInOutLine inOutLine) {

		// Check if Production only possible when sufficient quantity in stock
		String whereClause = "M_Product_ID=? and M_Locator_ID=? and Movementdate<=? ";
		ArrayList <Object> params = new ArrayList<>();
		params.add(inOutLine.getM_Product_ID());
		params.add(inOutLine.getM_Locator_ID());
		params.add(getMovementDate());
		if (inOutLine.getM_AttributeSetInstance_ID()!=0){
			whereClause = whereClause + " and M_AttributesetInstance_ID=? ";
			params.add(inOutLine.getM_AttributeSetInstance_ID());
		}
		BigDecimal qtyOnHand = new Query(getCtx(), MTransaction.Table_Name, whereClause, get_TrxName())
				.setParameters(params)
				.aggregate(MTransaction.COLUMNNAME_MovementQty, Query.AGGREGATE_SUM);

		if (qtyOnHand.add(inOutLine.getMovementQty()).compareTo(Env.ZERO)<0) {
			return (Msg.translate(getCtx(), "NotEnoughStocked") + " " + inOutLine.getM_Product().getName() 
					+ " Proyecto:" + inOutLine.getC_Project().getValue() + " " + inOutLine.getC_Project().getName()
					+ ": " + Msg.translate(getCtx(), "QtyAvailable") + " " + qtyOnHand.toString() + ".\n" );
		}
		return "";
	}
}
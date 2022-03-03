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

package org.compiere.process;


import java.util.List;

import org.compiere.model.MProjectTask;


/**
 *	Update Project Tasks
 *	
 *  
 * 	@author Mario Calderon, mario.calderon@westfalia-it.com, Systemhaus Westfalia http://www.westfalia-it.com
 * 
 */
public class ProjectUpdateTasks extends ProjectUpdateTasksAbstract
{
	protected List<MProjectTask> tasksFromBrowser = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		for(int projectTask_ID : getSelectionKeys()) {
			MProjectTask task = new MProjectTask(getCtx(), projectTask_ID, get_TrxName());
			task.setM_Product_ID(getSelectionAsInt(projectTask_ID, "PT_M_Product_ID"));
			task.setPriceList(getSelectionAsBigDecimal(projectTask_ID, "PT_PriceList"));
			task.setLineNetAmt(getSelectionAsBigDecimal(projectTask_ID, "PT_LineNetAmt"));
			task.setQty(getSelectionAsBigDecimal(projectTask_ID, "PT_Qty"));
			task.setPriceEntered(getSelectionAsBigDecimal(projectTask_ID, "PT_PriceEntered"));
			task.setActualAmt(getSelectionAsBigDecimal(projectTask_ID, "PT_ActualAmt"));
			task.setMarginAmt(getSelectionAsBigDecimal(projectTask_ID, "PT_MarginAmt"));
			task.setMargin(getSelectionAsBigDecimal(projectTask_ID, "PT_Margin"));
			task.saveEx();			
		}
		
		return "";
	}
}
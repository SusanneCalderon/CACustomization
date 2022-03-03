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

import org.compiere.model.MProjectPhase;


/**
 *	Update Project Tasks
 *	
 *  
 * 	@author Mario Calderon, mario.calderon@westfalia-it.com, Systemhaus Westfalia http://www.westfalia-it.com
 * 
 */
public class ProjectUpdatePhases extends ProjectUpdatePhasesAbstract
{
	protected List<MProjectPhase> tasksFromBrowser = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		for(int projectPhase_ID : getSelectionKeys()) {
			MProjectPhase phase = new MProjectPhase(getCtx(), projectPhase_ID, get_TrxName());
			phase.setM_Product_ID(getSelectionAsInt(projectPhase_ID, "PP_M_Product_ID"));
			phase.setPriceList(getSelectionAsBigDecimal(projectPhase_ID, "PP_PriceList"));
			phase.setLineNetAmt(getSelectionAsBigDecimal(projectPhase_ID, "PP_LineNetAmt"));
			phase.setQty(getSelectionAsBigDecimal(projectPhase_ID, "PP_Qty"));
			phase.setPriceEntered(getSelectionAsBigDecimal(projectPhase_ID, "PP_PriceEntered"));
			phase.setActualAmt(getSelectionAsBigDecimal(projectPhase_ID, "PP_ActualAmt"));
			phase.setMarginAmt(getSelectionAsBigDecimal(projectPhase_ID, "PP_MarginAmt"));
			phase.setMargin(getSelectionAsBigDecimal(projectPhase_ID, "PP_Margin"));
			phase.saveEx();			
		}
		
		return "";
	}
}
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

import org.compiere.model.MFactAcct;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.Query;

/** Generated Process for (C_Order_Process)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class UpdateProjectIsse_ProjectLine extends UpdateProjectIsse_ProjectLineAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{

		for(int key_ID : getSelectionKeys()) {
			MProjectIssue projectIssue = new MProjectIssue(getCtx(), key_ID, get_TrxName());
			projectIssue.setC_ProjectPhase_ID(getProjectPhaseId());
			if (getProjectTaskId() > 0)
				projectIssue.setC_ProjectTask_ID(getProjectTaskId());
			projectIssue.saveEx();
			MProjectLine projectLine = new Query(getCtx(), MProjectLine.Table_Name, "c_ProjectIssue_ID=?", get_TrxName())
					.setParameters(key_ID)
					.first();
			if (projectLine != null) {
				projectLine.setC_ProjectPhase_ID(getProjectPhaseId());
				projectLine.setC_ProjectTask_ID(getProjectTaskId());
				projectLine.saveEx();				
			}
		    List<MFactAcct> lines =  new Query(getCtx(), MFactAcct.Table_Name,
		    		"record_ID=? and ad_Table_ID = " + MProjectIssue.Table_ID, get_TrxName())
		    		.setParameters(key_ID)
		    		.list();
		    for (MFactAcct line:lines) {
		    	line.setC_ProjectPhase_ID(projectIssue.getC_ProjectPhase_ID());
		    	line.setC_ProjectTask_ID(projectIssue.getC_ProjectTask_ID());
		    	line.saveEx();
		    }
		}
		return "";
	}
}
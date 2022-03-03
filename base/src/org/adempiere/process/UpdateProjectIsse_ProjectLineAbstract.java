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

import org.compiere.process.SvrProcess;

/** Generated Process for (SB Update ProjectIsse and ProjectLine)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public abstract class UpdateProjectIsse_ProjectLineAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "UpdateProjectIsse_ProjectLine";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SB Update ProjectIsse and ProjectLine";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 1000052;
	/**	Parameter Name for Project Phase	*/
	public static final String C_PROJECTPHASE_ID = "C_ProjectPhase_ID";
	/**	Parameter Name for Project Task	*/
	public static final String C_PROJECTTASK_ID = "C_ProjectTask_ID";
	/**	Parameter Value for Project Phase	*/
	private int projectPhaseId;
	/**	Parameter Value for Project Task	*/
	private int projectTaskId;

	@Override
	protected void prepare() {
		projectPhaseId = getParameterAsInt(C_PROJECTPHASE_ID);
		projectTaskId = getParameterAsInt(C_PROJECTTASK_ID);
	}

	/**	 Getter Parameter Value for Project Phase	*/
	protected int getProjectPhaseId() {
		return projectPhaseId;
	}

	/**	 Setter Parameter Value for Project Phase	*/
	protected void setProjectPhaseId(int projectPhaseId) {
		this.projectPhaseId = projectPhaseId;
	}

	/**	 Getter Parameter Value for Project Task	*/
	protected int getProjectTaskId() {
		return projectTaskId;
	}

	/**	 Setter Parameter Value for Project Task	*/
	protected void setProjectTaskId(int projectTaskId) {
		this.projectTaskId = projectTaskId;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}
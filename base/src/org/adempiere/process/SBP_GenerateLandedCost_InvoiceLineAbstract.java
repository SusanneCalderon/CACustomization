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

/** Generated Process for (SBP_GenerateLandedCost_InvoiceLine)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class SBP_GenerateLandedCost_InvoiceLineAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_GenerateLandedCost_InvoiceLine";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_GenerateLandedCost_InvoiceLine";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54346;
	/**	Parameter Name for User List 4	*/
	public static final String USER4_ID = "User4_ID";
	/**	Parameter Name for Shipment/Receipt	*/
	public static final String M_INOUT_ID = "M_InOut_ID";
	/**	Parameter Name for Cost Distribution	*/
	public static final String LANDEDCOSTDISTRIBUTION = "LandedCostDistribution";
	/**	Parameter Name for Cost Element	*/
	public static final String M_COSTELEMENT_ID = "M_CostElement_ID";
	/**	Parameter Name for Landed Cost Type	*/
	public static final String C_LANDEDCOSTTYPE_ID = "C_LandedCostType_ID";
	/**	Parameter Value for User List 4	*/
	private int user4Id;
	/**	Parameter Value for Shipment/Receipt	*/
	private int inOutId;
	/**	Parameter Value for Cost Distribution	*/
	private String landedCostDistribution;
	/**	Parameter Value for Cost Element	*/
	private int costElementId;
	/**	Parameter Value for Landed Cost Type	*/
	private int landedCostTypeId;

	@Override
	protected void prepare() {
		user4Id = getParameterAsInt(USER4_ID);
		inOutId = getParameterAsInt(M_INOUT_ID);
		landedCostDistribution = getParameterAsString(LANDEDCOSTDISTRIBUTION);
		costElementId = getParameterAsInt(M_COSTELEMENT_ID);
		landedCostTypeId = getParameterAsInt(C_LANDEDCOSTTYPE_ID);
	}

	/**	 Getter Parameter Value for User List 4	*/
	protected int getUser4Id() {
		return user4Id;
	}

	/**	 Setter Parameter Value for User List 4	*/
	protected void setUser4Id(int user4Id) {
		this.user4Id = user4Id;
	}

	/**	 Getter Parameter Value for Shipment/Receipt	*/
	protected int getInOutId() {
		return inOutId;
	}

	/**	 Setter Parameter Value for Shipment/Receipt	*/
	protected void setInOutId(int inOutId) {
		this.inOutId = inOutId;
	}

	/**	 Getter Parameter Value for Cost Distribution	*/
	protected String getLandedCostDistribution() {
		return landedCostDistribution;
	}

	/**	 Setter Parameter Value for Cost Distribution	*/
	protected void setLandedCostDistribution(String landedCostDistribution) {
		this.landedCostDistribution = landedCostDistribution;
	}

	/**	 Getter Parameter Value for Cost Element	*/
	protected int getCostElementId() {
		return costElementId;
	}

	/**	 Setter Parameter Value for Cost Element	*/
	protected void setCostElementId(int costElementId) {
		this.costElementId = costElementId;
	}

	/**	 Getter Parameter Value for Landed Cost Type	*/
	protected int getLandedCostTypeId() {
		return landedCostTypeId;
	}

	/**	 Setter Parameter Value for Landed Cost Type	*/
	protected void setLandedCostTypeId(int landedCostTypeId) {
		this.landedCostTypeId = landedCostTypeId;
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
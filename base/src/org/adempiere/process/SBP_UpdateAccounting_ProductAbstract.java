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

/** Generated Process for (SBP_UpdateAccounting_Product)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class SBP_UpdateAccounting_ProductAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_UpdateAccounting_Product";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_UpdateAccounting_Product";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54348;
	/**	Parameter Name for Product Asset	*/
	public static final String P_ASSET_ACCT = "P_Asset_Acct";
	/**	Parameter Name for Product COGS	*/
	public static final String P_COGS_ACCT = "P_COGS_Acct";
	/**	Parameter Name for Product Revenue	*/
	public static final String P_REVENUE_ACCT = "P_Revenue_Acct";
	/**	Parameter Name for Product Expense	*/
	public static final String P_EXPENSE_ACCT = "P_Expense_Acct";
	/**	Parameter Value for Product Asset	*/
	private int assetAcctId;
	/**	Parameter Value for Product COGS	*/
	private int cOGSAcctId;
	/**	Parameter Value for Product Revenue	*/
	private int revenueAcctId;
	/**	Parameter Value for Product Expense	*/
	private int expenseAcctId;

	@Override
	protected void prepare() {
		assetAcctId = getParameterAsInt(P_ASSET_ACCT);
		cOGSAcctId = getParameterAsInt(P_COGS_ACCT);
		revenueAcctId = getParameterAsInt(P_REVENUE_ACCT);
		expenseAcctId = getParameterAsInt(P_EXPENSE_ACCT);
	}

	/**	 Getter Parameter Value for Product Asset	*/
	protected int getAssetAcctId() {
		return assetAcctId;
	}

	/**	 Setter Parameter Value for Product Asset	*/
	protected void setAssetAcctId(int assetAcctId) {
		this.assetAcctId = assetAcctId;
	}

	/**	 Getter Parameter Value for Product COGS	*/
	protected int getCOGSAcctId() {
		return cOGSAcctId;
	}

	/**	 Setter Parameter Value for Product COGS	*/
	protected void setCOGSAcctId(int cOGSAcctId) {
		this.cOGSAcctId = cOGSAcctId;
	}

	/**	 Getter Parameter Value for Product Revenue	*/
	protected int getRevenueAcctId() {
		return revenueAcctId;
	}

	/**	 Setter Parameter Value for Product Revenue	*/
	protected void setRevenueAcctId(int revenueAcctId) {
		this.revenueAcctId = revenueAcctId;
	}

	/**	 Getter Parameter Value for Product Expense	*/
	protected int getExpenseAcctId() {
		return expenseAcctId;
	}

	/**	 Setter Parameter Value for Product Expense	*/
	protected void setExpenseAcctId(int expenseAcctId) {
		this.expenseAcctId = expenseAcctId;
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
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

/** Generated Process for (SBP_PaySelectionPrintAndPay)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class Print_PayselectionAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_PaySelectionPrintAndPay";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_PaySelectionPrintAndPay";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54339;
	/**	Parameter Name for Current Next	*/
	public static final String CURRENTNEXT = "CurrentNext";
	/**	Parameter Name for Bank Account Document	*/
	public static final String C_BANKACCOUNTDOC_ID = "C_BankAccountDoc_ID";
	/**	Parameter Name for cmd_PayPrint	*/
	public static final String CMD_PAYPRINT = "cmd_PayPrint";
	/**	Parameter Value for Current Next	*/
	private int currentNext;
	/**	Parameter Value for Bank Account Document	*/
	private int bankAccountDocId;
	/**	Parameter Value for cmd_PayPrint	*/
	private String payPrint;

	@Override
	protected void prepare() {
		currentNext = getParameterAsInt(CURRENTNEXT);
		bankAccountDocId = getParameterAsInt(C_BANKACCOUNTDOC_ID);
		payPrint = getParameterAsString(CMD_PAYPRINT);
	}

	/**	 Getter Parameter Value for Current Next	*/
	protected int getCurrentNext() {
		return currentNext;
	}

	/**	 Setter Parameter Value for Current Next	*/
	protected void setCurrentNext(int currentNext) {
		this.currentNext = currentNext;
	}

	/**	 Getter Parameter Value for Bank Account Document	*/
	protected int getBankAccountDocId() {
		return bankAccountDocId;
	}

	/**	 Setter Parameter Value for Bank Account Document	*/
	protected void setBankAccountDocId(int bankAccountDocId) {
		this.bankAccountDocId = bankAccountDocId;
	}

	/**	 Getter Parameter Value for cmd_PayPrint	*/
	protected String getPayPrint() {
		return payPrint;
	}

	/**	 Setter Parameter Value for cmd_PayPrint	*/
	protected void setPayPrint(String payPrint) {
		this.payPrint = payPrint;
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
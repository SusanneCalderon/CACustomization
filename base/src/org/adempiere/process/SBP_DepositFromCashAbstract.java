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
import java.sql.Timestamp;
import org.compiere.process.SvrProcess;

/** Generated Process for (SBP_DepositFromCash)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class SBP_DepositFromCashAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_DepositFromCash";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_DepositFromCash";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54347;
	/**	Parameter Name for Split Deposits	*/
	public static final String ISSPLITDEPOSITS = "IsSplitDeposits";
	/**	Parameter Name for Transaction Date	*/
	public static final String DATETRX = "DateTrx";
	/**	Parameter Name for Bank Account	*/
	public static final String C_BANKACCOUNT_ID = "C_BankAccount_ID";
	/**	Parameter Name for Charge	*/
	public static final String C_CHARGE_ID = "C_Charge_ID";
	/**	Parameter Name for Business Partner 	*/
	public static final String C_BPARTNER_ID = "C_BPartner_ID";
	/**	Parameter Name for Document No	*/
	public static final String DOCUMENTNO = "DocumentNo";
	/**	Parameter Name for Payment amount	*/
	public static final String PAYAMT = "PayAmt";
	/**	Parameter Name for Tender type	*/
	public static final String TENDERTYPE = "TenderType";
	/**	Parameter Name for Cheque No	*/
	public static final String CHEQUENO = "ChequeNo";
	/**	Parameter Value for Split Deposits	*/
	private boolean isSplitDeposits;
	/**	Parameter Value for Transaction Date	*/
	private Timestamp dateTrx;
	/**	Parameter Value for Bank Account	*/
	private int bankAccountId;
	/**	Parameter Value for Charge	*/
	private int chargeId;
	/**	Parameter Value for Business Partner 	*/
	private int bPartnerId;
	/**	Parameter Value for Document No	*/
	private String documentNo;
	/**	Parameter Value for Payment amount	*/
	private BigDecimal payAmt;
	/**	Parameter Value for Tender type	*/
	private String tenderType;
	/**	Parameter Value for Cheque No	*/
	private String chequeNo;

	@Override
	protected void prepare() {
		isSplitDeposits = getParameterAsBoolean(ISSPLITDEPOSITS);
		dateTrx = getParameterAsTimestamp(DATETRX);
		bankAccountId = getParameterAsInt(C_BANKACCOUNT_ID);
		chargeId = getParameterAsInt(C_CHARGE_ID);
		bPartnerId = getParameterAsInt(C_BPARTNER_ID);
		documentNo = getParameterAsString(DOCUMENTNO);
		payAmt = getParameterAsBigDecimal(PAYAMT);
		tenderType = getParameterAsString(TENDERTYPE);
		chequeNo = getParameterAsString(CHEQUENO);
	}

	/**	 Getter Parameter Value for Split Deposits	*/
	protected boolean isSplitDeposits() {
		return isSplitDeposits;
	}

	/**	 Setter Parameter Value for Split Deposits	*/
	protected void setIsSplitDeposits(boolean isSplitDeposits) {
		this.isSplitDeposits = isSplitDeposits;
	}

	/**	 Getter Parameter Value for Transaction Date	*/
	protected Timestamp getDateTrx() {
		return dateTrx;
	}

	/**	 Setter Parameter Value for Transaction Date	*/
	protected void setDateTrx(Timestamp dateTrx) {
		this.dateTrx = dateTrx;
	}

	/**	 Getter Parameter Value for Bank Account	*/
	protected int getBankAccountId() {
		return bankAccountId;
	}

	/**	 Setter Parameter Value for Bank Account	*/
	protected void setBankAccountId(int bankAccountId) {
		this.bankAccountId = bankAccountId;
	}

	/**	 Getter Parameter Value for Charge	*/
	protected int getChargeId() {
		return chargeId;
	}

	/**	 Setter Parameter Value for Charge	*/
	protected void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}

	/**	 Getter Parameter Value for Business Partner 	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Business Partner 	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for Document No	*/
	protected String getDocumentNo() {
		return documentNo;
	}

	/**	 Setter Parameter Value for Document No	*/
	protected void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}

	/**	 Getter Parameter Value for Payment amount	*/
	protected BigDecimal getPayAmt() {
		return payAmt;
	}

	/**	 Setter Parameter Value for Payment amount	*/
	protected void setPayAmt(BigDecimal payAmt) {
		this.payAmt = payAmt;
	}

	/**	 Getter Parameter Value for Tender type	*/
	protected String getTenderType() {
		return tenderType;
	}

	/**	 Setter Parameter Value for Tender type	*/
	protected void setTenderType(String tenderType) {
		this.tenderType = tenderType;
	}

	/**	 Getter Parameter Value for Cheque No	*/
	protected String getChequeNo() {
		return chequeNo;
	}

	/**	 Setter Parameter Value for Cheque No	*/
	protected void setChequeNo(String chequeNo) {
		this.chequeNo = chequeNo;
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
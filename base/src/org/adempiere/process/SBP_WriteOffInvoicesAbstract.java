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

/** Generated Process for (SBP_WriteOffInvoices)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class SBP_WriteOffInvoicesAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_WriteOffInvoices";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_WriteOffInvoices";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54386;
	/**	Parameter Name for Account Date	*/
	public static final String DATEACCT = "DateAcct";
	/**	Parameter Name for Charge	*/
	public static final String C_CHARGE_ID = "C_Charge_ID";
	/**	Parameter Name for Amount	*/
	public static final String AMOUNT = "Amount";
	/**	Parameter Value for Account Date	*/
	private Timestamp dateAcct;
	/**	Parameter Value for Charge	*/
	private int charge;
	/**	Parameter Value for Amount	*/
	private BigDecimal amount;

	@Override
	protected void prepare() {
		dateAcct = getParameterAsTimestamp(DATEACCT);
		charge = getParameterAsInt(C_CHARGE_ID);
		amount = getParameterAsBigDecimal(AMOUNT);
	}

	/**	 Getter Parameter Value for Account Date	*/
	protected Timestamp getDateAcct() {
		return dateAcct;
	}

	/**	 Setter Parameter Value for Account Date	*/
	protected void setDateAcct(Timestamp dateAcct) {
		this.dateAcct = dateAcct;
	}

	/**	 Getter Parameter Value for Charge	*/
	protected int getCharge() {
		return charge;
	}

	/**	 Setter Parameter Value for Charge	*/
	protected void setCharge(int charge) {
		this.charge = charge;
	}

	/**	 Getter Parameter Value for Amount	*/
	protected BigDecimal getAmount() {
		return amount;
	}

	/**	 Setter Parameter Value for Amount	*/
	protected void setAmount(BigDecimal amount) {
		this.amount = amount;
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
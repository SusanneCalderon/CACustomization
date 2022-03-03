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

/** Generated Process for (SB_PaymentPrint)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class SB_PaymentPrintAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SB_PaymentPrint";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SB_PaymentPrint";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 1000103;
	/**	Parameter Name for Printer Name	*/
	public static final String PRINTERNAME = "PrinterName";
	/**	Parameter Value for Printer Name	*/
	private String printerName;

	@Override
	protected void prepare() {
		printerName = getParameterAsString(PRINTERNAME);
	}

	/**	 Getter Parameter Value for Printer Name	*/
	protected String getPrinterName() {
		return printerName;
	}

	/**	 Setter Parameter Value for Printer Name	*/
	protected void setPrinterName(String printerName) {
		this.printerName = printerName;
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
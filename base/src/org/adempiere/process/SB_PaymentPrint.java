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

import org.compiere.print.CPrinter;
import org.compiere.print.ReportCtl;
import org.compiere.util.Env;
import org.compiere.util.Ini;

/** Generated Process for (SB_PaymentPrint)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SB_PaymentPrint extends SB_PaymentPrintAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		CPrinter fPrinter = new CPrinter();
		String printerName = "MatrixDruckerEpson";
        Env.setContext(getCtx(), "#Printer",  printerName);
        Ini.setProperty(Ini.P_PRINTER, printerName);
		getSelectionKeys().stream().forEach(key ->{
			boolean ok = ReportCtl.startCheckPrint(key, true);
			
		});
		return "";
	}
}
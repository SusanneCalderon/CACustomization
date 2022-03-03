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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//import org.apache.commons.net.ntp.TimeStamp;
import org.compiere.model.MFactAcct;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MPeriod;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/** Generated Process for (C_Order_Process)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class TemplateJavaScript extends SvrProcess
{
	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{
		Properties A_Ctx = getCtx();
		String A_TrxName = get_TrxName();
		int A_Record_ID = getRecord_ID();
		
		Timestamp P_DateAcct = null;
		
		
		MInvoice invoice = new MInvoice(A_Ctx, A_Record_ID, A_TrxName);
		if (!MPeriod.isOpen(getCtx(), invoice.getDateAcct(), invoice.getC_DocType().getDocBaseType(), invoice.getAD_Org_ID()))
			return "Periodo cerrado";
		ArrayList<Object> params = new ArrayList<>();
		params.add(A_Record_ID);
		params.add(MInvoice.Table_ID);
		String sqlDelete = "Delete from fact_Acct where record_ID=? and ad_Table_ID=?";
		int no = DB.executeUpdateEx(sqlDelete, params.toArray(), A_TrxName);
		invoice.setDateAcct(P_DateAcct);
		//invoice.setDateInvoiced(null);
		invoice.saveEx();
		return "";
	}
}
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

import java.util.ArrayList;

import org.compiere.util.DB;

/** Generated Process for (T_BankTransaction_Report)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class T_BankTransaction_Report extends T_BankTransaction_ReportAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		StringBuffer sqlBalance = new StringBuffer("INSERT INTO T_BankTransaction");
		sqlBalance.append("(AD_PInstance_ID, AD_Client_ID, datetrx , c_bankaccount_id,reconciledPayAmt,notreconciledPayAmt, description)");
		sqlBalance.append(" Select ?, AD_Client_ID, ? as datetrx ,c_bankaccount_id,sum(reconciledPayAmt),");
		sqlBalance.append(" sum(notreconciledPayAmt), 'Saldo Inicial' as description from RV_PaymentReport_Accumulated where  ");
		sqlBalance.append(" c_Bankaccount_ID = ? and docstatus = 'CO' and datetrx <= ?   "
				+ " GROUP by c_Bankaccount_ID, docstatus, AD_Client_ID, AD_Org_ID ");
		ArrayList<Object> params = new ArrayList<>();
		params.add(getAD_PInstance_ID());
		params.add(getDateTrx());
		params.add(getBankAccountId());
		params.add(getDateTrx());
		int no = DB.executeUpdateEx(sqlBalance.toString(), params.toArray(), get_TrxName());
		StringBuffer sqlDetail = new StringBuffer("INSERT INTO T_BankTransaction(AD_Pinstance_ID, AD_Client_ID, AD_Org_ID, datetrx ,c_bankaccount_id," +
				"documentno, checkno, c_Bpartner_ID, reconciledPayAmt,tendertype,isreceipt,overunderamt,isprepayment,c_Project_ID," +
				" notreconciledPayAmt, paidInvoice_ID,allocatedcharge_ID, transferFrom_ID, description)");
		sqlDetail.append("Select ?, AD_Client_ID, AD_Org_ID, datetrx ,c_bankaccount_id,documentno, checkno, c_Bpartner_ID," +
				 " reconciledPayAmt,tendertype,isreceipt,overunderamt,isprepayment,c_Project_ID, " +
				 " notreconciledPayAmt, paidInvoice_ID,allocatedcharge_ID, transferFrom_ID, description  from RV_PaymentReport "+
				 " where  docstatus = 'CO'  and c_Bankaccount_ID=? And datetrx Between ? and ? ");
		params.clear();
		params.add(getAD_PInstance_ID());
		params.add(getBankAccountId());
		params.add(getDateTrx());
		params.add(getDateTrxTo());
		no = DB.executeUpdateEx(sqlDetail.toString(), params.toArray(), get_TrxName());
		return "";
	}
}
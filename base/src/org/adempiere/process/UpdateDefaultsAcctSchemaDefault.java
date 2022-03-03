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
import java.sql.SQLException;
import java.sql.Timestamp;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MPayment;
import org.compiere.util.Env;

/** Generated Process for (UpdateDefaultsAcctSchemaDefault)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class UpdateDefaultsAcctSchemaDefault extends UpdateDefaultsAcctSchemaDefaultAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception, SQLException
	{

		getSelectionKeys().stream().forEach( key -> {
			int account_ID 	= getSelectionAsInt(key, "VC_Account_ID");
			String columnName = getSelectionAsString(key, "C_ColumnName");
			int c_AcctSchema_ID = getSelectionAsInt(key, "VC_C_AcctSchema_ID");//	
			
			MAccount account = MAccount.get(getCtx(), Env.getAD_Client_ID(getCtx()), 
					0, c_AcctSchema_ID, account_ID, 
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,get_TrxName());
			try {
				commitEx();				
			} catch (Exception e) {
				// TODO: handle exception
			}
			MAcctSchemaDefault acctSchemaDefault = MAcctSchemaDefault.get(getCtx(), c_AcctSchema_ID);
			acctSchemaDefault.set_CustomColumn(columnName, account.getC_ValidCombination_ID());
			acctSchemaDefault.saveEx();

			//	Add to created
		});   //  for all rows
		return "";
	}
}
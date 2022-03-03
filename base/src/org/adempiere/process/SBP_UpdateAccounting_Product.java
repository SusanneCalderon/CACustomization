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
import java.util.Properties;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MBPartner;
import org.compiere.model.ProductCost;
import org.compiere.util.DB;
import org.compiere.util.Env;

/** Generated Process for (SBP_UpdateAccounting_Product)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_UpdateAccounting_Product extends SBP_UpdateAccounting_ProductAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		for (MAcctSchema acctSchema: MAcctSchema.getClientAcctSchema(getCtx(), Env.getAD_Client_ID(getCtx()), get_TrxName())) {
			int acctSchemaID = acctSchema.get_ID();
			MAccount accountAsset = MAccount.get (getCtx(), 
					Env.getAD_Client_ID(getCtx()), 0, acctSchemaID,
					getAssetAcctId()
					, 0,0, 0, 0,0, 0, 0,0, 0, 0,0, 0, 0, 0 , 0, 0, get_TrxName());

			MAccount accountExpense = MAccount.get (getCtx(), 
					Env.getAD_Client_ID(getCtx()), 0, acctSchemaID,
					getExpenseAcctId()
					, 0,0, 0, 0,0, 0, 0,0, 0, 0, 0, 0, 0, 0 , 0, 0, get_TrxName());

			MAccount accountCOGS = MAccount.get (getCtx(), 
					Env.getAD_Client_ID(getCtx()), 0, acctSchemaID,
					getCOGSAcctId()
					, 0,0, 0, 0,0, 0, 0,0, 0, 0, 0, 0, 0, 0 , 0, 0, get_TrxName());

			MAccount accountRevenue = MAccount.get (getCtx(), 
					Env.getAD_Client_ID(getCtx()), 0, acctSchemaID,
					getRevenueAcctId()
					, 0,0, 0, 0,0, 0, 0,0, 0, 0, 0, 0, 0, 0 , 0, 0, get_TrxName());
			StringBuffer updateAcct = new StringBuffer(" UPDATE M_Product_Acct set ");
			updateAcct.append(MAcctSchemaDefault.COLUMNNAME_P_Asset_Acct  + "= " + accountAsset.getC_ValidCombination_ID() + ",");
			updateAcct.append(MAcctSchemaDefault.COLUMNNAME_P_Expense_Acct+ "= " + accountExpense.getC_ValidCombination_ID() + ",");
			updateAcct.append(MAcctSchemaDefault.COLUMNNAME_P_COGS_Acct+ "= " + accountCOGS.getC_ValidCombination_ID() + ",");
			updateAcct.append(MAcctSchemaDefault.COLUMNNAME_P_Revenue_Acct+ "= " + accountRevenue.getC_ValidCombination_ID() );
			updateAcct.append(" WHERE M_Product_ID=? AND C_AcctSchema_ID=?");
			for (Integer key : getSelectionKeys()) {
				int no = DB.executeUpdateEx(updateAcct.toString(),new	Object[]{key, acctSchemaID},get_TrxName());					
			}	
		}			
		return "";
	}
	
}
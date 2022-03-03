/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2016 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
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
import java.util.List;
import java.util.Properties;

import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.Query;
import org.compiere.util.Env;

/** Generated Process for (SB_BankStatementUpdate)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class SBP_BankStatementUpdate extends SBP_BankStatementUpdateAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
    	Properties A_Ctx = getCtx();
    	String A_TrxName = get_TrxName();
    	List<MBankStatement> m_records = null;
    	MBankStatement oldStatement = null;
    	MBankAccount ba = null;
    	Boolean first = true;
    	BigDecimal totalamount = Env.ZERO;
    	String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE  T_Selection.AD_PInstance_ID=? " +
				" AND T_Selection.T_Selection_ID=C_BankStatement.C_BankStatement_ID)";
		m_records = new Query(A_Ctx, MBankStatement.Table_Name, whereClause, A_TrxName)
											.setParameters(getAD_PInstance_ID())
											.setOrderBy("statementdate, c_bankstatement_ID")
											.setClient_ID()
											.list();

		BigDecimal oldSaldo = Env.ZERO;
		for (MBankStatement bs:m_records)
		{
			if (first)
			{
				oldSaldo = getBeginningBalance();
				ba = bs.getBankAccount();
				totalamount = totalamount.add(getBeginningBalance());
				ba.saveEx();
				first = false;
			}
			/*if (oldStatement == null)
			{
				oldStatement = new Query(A_Ctx, MBankStatement.Table_Name, "C_Bankaccount_ID=? and statementdate <=? ", A_TrxName)
						.setOrderBy("statementdate desc, C_Bankstatement_ID desc")
						.first();
				if (oldStatement != null)
					oldSaldo = oldStatement.getEndingBalance();				
			}*/
			bs.setBeginningBalance(oldSaldo);
			bs.setEndingBalance(bs.getBeginningBalance().add(bs.getStatementDifference()));
			bs.saveEx();
			oldSaldo = bs.getEndingBalance();
			totalamount = totalamount.add(bs.getStatementDifference());
		}
		ba.load(get_TrxName());
		//BF 1933645
		ba.setCurrentBalance(totalamount);
		ba.saveEx();
    	
    	return "";    	    
    
	}
}
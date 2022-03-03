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

import org.compiere.model.I_C_BankAccount;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MPayment;
import org.compiere.util.Env;

/** Generated Process for (CreateBankTransfer_Prov)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class CreateBankTransfer_Prov extends CreateBankTransfer_ProvAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		//BigDecimal percent = getPercent().compareTo(Env.ZERO) !=0? getPercent().divide(Env.ONEHUNDRED):Env.ZERO;
		BigDecimal diff = getPercent();
		BigDecimal amount = getTransactionAmount();
		MBankStatement bankStatement = new MBankStatement(getCtx(), getRecord_ID(), get_TrxName());
		MBankStatementLine bankStatementLine = new MBankStatementLine(bankStatement);
		bankStatementLine.set_ValueOfColumn("C_BankAccount_ID", getBankAccountId());
		bankStatementLine.setStatementLineDate(bankStatement.getStatementDate());
		bankStatementLine.setDateAcct(bankStatement.getStatementDate());
		bankStatementLine.set_ValueOfColumn("ControlAmt", amount.negate());
		bankStatementLine.setTrxAmt(amount.negate());
		bankStatementLine.setStmtAmt(amount.negate());
		bankStatementLine.saveEx();
		if (getPercent().compareTo(Env.ZERO) != 0)
		{
			bankStatementLine = new MBankStatementLine(bankStatement);			
			bankStatementLine.setStatementLineDate(bankStatement.getStatementDate());
			bankStatementLine.setDateAcct(bankStatement.getStatementDate());
			bankStatementLine.setTrxAmt(diff.negate());
			bankStatementLine.setStmtAmt(diff.negate());
			MBankAccount bankAccount = new MBankAccount(getCtx(), getBankAccountId(), get_TrxName());
			bankStatementLine.setC_BPartner_ID(bankAccount.getC_Bank().getC_BPartner_ID());
			bankStatementLine.saveEx();
		}
		return "";
	}
}
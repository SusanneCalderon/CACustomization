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
import java.util.concurrent.atomic.AtomicReference;

import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;

/** Generated Process for (SBP_AllocateDiffBPartner)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_AllocateDiffBPartner extends SBP_AllocateDiffBPartnerAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		MInvoice creditMemo = new MInvoice(getCtx(), getInvoiceId(), get_TrxName());
		MAllocationHdr allocationHdr = new MAllocationHdr(getCtx(), false, Env.getContextAsDate(getCtx(), "#Date"), 100, 
				"", get_TrxName());
		allocationHdr.setAD_Org_ID(creditMemo.getAD_Org_ID());
		allocationHdr.setDateAcct(allocationHdr.getDateTrx()); // in case date acct is different from datetrx in payment
		allocationHdr.saveEx();
		AtomicReference<BigDecimal> creditMemoOpenAmt = new AtomicReference<>(creditMemo.getOpenAmt());
		AtomicReference<BigDecimal> allocatedAmt = new AtomicReference<>(Env.ZERO);
		 getSelectionKeys().stream().forEach(key -> {
		        MInvoice invoice = new MInvoice(getCtx(), key, get_TrxName());

				final BigDecimal allocationAmt = getSelectionAsBigDecimal(key, "OI_OpenAmt").compareTo(creditMemoOpenAmt.get().negate()) <=0?
						getSelectionAsBigDecimal(key, "OI_OpenAmt"): creditMemoOpenAmt.get();       
		        MAllocationLine allocationLine = new MAllocationLine(allocationHdr);
		        allocationLine.setC_Charge_ID(getCharge());
		        allocationLine.setAmount(allocationAmt);
		        allocationLine.setC_Invoice_ID(invoice.getC_Invoice_ID());
		        allocationLine.saveEx();
		        creditMemoOpenAmt.updateAndGet(balance -> balance.add(allocationAmt));
		        allocatedAmt.updateAndGet(balance -> balance.add(allocationAmt));
		    });
		 MAllocationLine allocationLine = new MAllocationLine(allocationHdr);
		 allocationLine.setC_Invoice_ID(creditMemo.getC_Invoice_ID());
		 allocationLine.setC_Charge_ID(getCharge());
		 allocationLine.setAmount(allocatedAmt.get().negate());
	     allocationLine.saveEx();
		return allocationHdr.getDocumentInfo();
	}
}
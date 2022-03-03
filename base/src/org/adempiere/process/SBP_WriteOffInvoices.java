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
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MCostDetail;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPayment;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.eevolution.service.dsl.ProcessBuilder;

/** Generated Process for (SBP_WriteOffInvoices)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_WriteOffInvoices extends SBP_WriteOffInvoicesAbstract
{

	/**	Allocation Hdr			*/
	private MAllocationHdr allocation = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{ getSelectionKeys().stream().forEach(invoiceId -> {
        MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());
        String error = WriteOffInvoice(invoice);
        //if (landedCost.getC_InvoiceLine().getC_Invoice().getDocStatus().equals("CO"))
		//	generateCostDetail(landedCost);	
    });
	return "";
	}
	
	private String WriteOffInvoice(MInvoice invoice) {
		/*ProcessInfo WriteOffInvoice = ProcessBuilder.create(getCtx())
                .process(171)
                .withTitle("WriteOffInvoice")
                .withParameter(MInvoice.COLUMNNAME_C_Invoice_ID, invoice.getC_Invoice_ID())
                .withParameter("MaxInvWriteOffAmt", Env.ONE)
                .withParameter("IsSimulation", false)
                .withParameter("DateInvoiced", Env.getContext(getCtx(), "@#Date@"))
                .withoutTransactionClose()
                .execute(get_TrxName());
		if (WriteOffInvoice.isError())
			throw new AdempiereException(WriteOffInvoice.getSummary());
		*/
		writeOff(invoice.getC_Invoice_ID(), invoice.getDocumentNo(), invoice.getDateAcct(), invoice.getC_Currency_ID(), getAmount());
	 
		return "";
	}
	
	private boolean writeOff (int invoiceId, String documentNo, Timestamp dateInvoiced,
			int currencyId, BigDecimal openAmt)
		{
			
			
			//	Invoice
			MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());
			if (!invoice.isSOTrx()) {
				openAmt = openAmt.negate();				
			}
			
			//	Allocation
			if (allocation == null || currencyId != allocation.getC_Currency_ID())
			{
				processAllocation();
				allocation = new MAllocationHdr(getCtx(), true, 
					getDateAcct(), currencyId,
					getProcessInfo().getTitle() + " #" + documentNo, get_TrxName());
				allocation.setAD_Org_ID(invoice.getAD_Org_ID());
				if (!allocation.save())
				{
					log.log(Level.SEVERE, "Cannot create allocation header");
					return false;
				}
			}

			//	Line
			MAllocationLine allocationLine = null;
			allocationLine = new MAllocationLine (allocation, Env.ZERO,
					Env.ZERO, openAmt, Env.ZERO);
			allocationLine.setC_Invoice_ID(invoiceId);
			allocationLine.setC_Charge_ID(getCharge());
			if (allocationLine.save())
			{
				addLog(invoiceId, dateInvoiced, openAmt, documentNo);
				return true;
			}
			//	Error
			log.log(Level.SEVERE, "Cannot create allocation line for C_Invoice_ID=" + invoiceId);
			return false;
		}	//	writeOff
		
		/**
		 * 	Process Allocation
		 *	@return true if processed
		 */
		private boolean processAllocation()
		{
			if (allocation == null)
				return true;
			
			//	Process It
			if (allocation.processIt(DocAction.ACTION_Complete) &&  allocation.save())
			{
				allocation = null;
				return true;
			}
			//
			allocation = null;
			return false;
		}	//	processAllocation

}
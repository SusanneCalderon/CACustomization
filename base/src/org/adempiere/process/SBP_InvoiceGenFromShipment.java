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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/** Generated Process for (SBP_InvoiceGenFromShipment)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_InvoiceGenFromShipment extends SBP_InvoiceGenFromShipmentAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{

		AtomicInteger counterAll = new AtomicInteger(0);
		AtomicInteger counterCreated = new AtomicInteger(0);
    	int AD_Process_ID = InvoiceGenerateFromShipmentAbstract.getProcessId();
    	Trx dbTransaction = Trx.get(get_TrxName(), true);   
    	getSelectionKeys().stream().forEach(key ->{
    		ProcessInfo pi = new ProcessInfo(InvoiceGenerateFromShipment.getProcessName(), AD_Process_ID);
    		pi.addParameter("M_InOut_ID", key, null);
    		pi.addParameter("AD_Org_ID", Env.getAD_Org_ID(getCtx()), null);
    		pi.addParameter("DocAction", getDocAction(), null);
    		InvoiceGenerateFromShipment invoiceGenerateFromShipment = new InvoiceGenerateFromShipment();
    		counterAll.updateAndGet(no -> no + 1);
    		Boolean success = invoiceGenerateFromShipment.startProcess(getCtx(), pi, dbTransaction);
    		if (!pi.getSummary().contains("0")) {
    			counterCreated.updateAndGet(no -> no + 1);
    		}    		
    	});
    	String msg =  "@Created@ = " + counterCreated + " @of@ " + counterAll;    	
		return msg;
	}
}
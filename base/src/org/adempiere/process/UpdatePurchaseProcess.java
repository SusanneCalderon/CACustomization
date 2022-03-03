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

import java.util.List;

import org.compiere.model.I_C_InvoiceLine;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_InOutLine;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;

/** Generated Process for (Update Purchase Process)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class UpdatePurchaseProcess extends UpdatePurchaseProcessAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		int table_ID = getTable_ID();
		if (table_ID == 259) {
			MOrder order = new MOrder(getCtx(), getRecord_ID(), get_TrxName());
			//order.setC_PurchaseProcess_ID(getPurchaseProcessId());
			order.saveEx();
			for (MOrderLine orderLine:order.getLines()) {
				//orderLine.setC_PurchaseProcess_ID(getPurchaseProcessId());
				orderLine.saveEx();
				String whereClause = I_C_OrderLine.COLUMNNAME_C_OrderLine_ID+"=?";
				List<MInvoiceLine> invoiceLines = new Query(getCtx(),I_C_InvoiceLine.Table_Name,whereClause,get_TrxName())
						.setParameters(orderLine.getC_OrderLine_ID())
						.list();
				invoiceLines.stream().filter(invoiceLine -> invoiceLine != null).forEach(invoiceLine -> {
					//invoiceLine.setC_PurchaseProcess_ID(invoiceLine.getC_OrderLine().getC_PurchaseProcess_ID());
					invoiceLine.saveEx();
				});

				whereClause = I_C_OrderLine.COLUMNNAME_C_OrderLine_ID+"=?";
				List<MInOutLine> inOutLines = new Query(getCtx(),I_M_InOutLine.Table_Name,whereClause,get_TrxName())
						.setParameters(orderLine.getC_OrderLine_ID())
						.list();
				inOutLines.stream().filter(inOutLine -> inOutLine != null).forEach(inOutLine -> {
					//inOutLine.setC_PurchaseProcess_ID(inOutLine.getC_OrderLine().getC_PurchaseProcess_ID());
					inOutLine.saveEx();
				});
			}
		}
		else if (table_ID == 318) {
			MInvoice invoice = new MInvoice(getCtx(), getRecord_ID(), get_TrxName());
			if (invoice.getC_Order_ID() != 0)
				return "Update Purchase Order";
			//invoice.setC_PurchaseProcess_ID(getPurchaseProcessId());
			invoice.saveEx();
			for (MInvoiceLine invoiceLine:invoice.getLines()) {
				if (invoiceLine.getC_OrderLine_ID() != 0)
					return "Update Purchase Order";
				//invoiceLine.setC_PurchaseProcess_ID(getPurchaseProcessId());
				invoiceLine.saveEx();
			}

		}

		return "";
	}
}
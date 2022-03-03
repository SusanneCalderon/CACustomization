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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MPayment;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/** Generated Process for (SBP_PaymentAllocateSelectionn)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_PaymentAllocateSelection extends SBP_PaymentAllocateSelectionAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		getSelectionKeys().stream().forEach(paymentId -> {
			MPayment payment = new MPayment(getCtx(), paymentId, get_TrxName());
			Boolean isAllocated = payment.testAllocation();
			if (isAllocated)
				payment.saveEx();
			else {				
			
			MAllocationHdr alloc = new MAllocationHdr(getCtx(), false, 
					payment.getDateTrx(), payment.getC_Currency_ID(),
					Msg.translate(getCtx(), "C_Payment_ID")	+ ": " + payment.getDocumentNo() + " [n]", get_TrxName());
				alloc.setAD_Org_ID(payment.getAD_Org_ID());
				alloc.setDateAcct(payment.getDateAcct()); // in case date acct is different from datetrx in payment
				
				String sql = "SELECT psc.C_BPartner_ID, psl.C_Invoice_ID, psl.IsSOTrx, "	//	1..3
					+ " psl.PayAmt, psl.DiscountAmt, psl.DifferenceAmt, psl.OpenAmt "
					+ "FROM C_PaySelectionLine psl"
					+ " INNER JOIN C_PaySelectionCheck psc ON (psl.C_PaySelectionCheck_ID=psc.C_PaySelectionCheck_ID) "
					+ " INNER JOIN C_Payment p ON (psc.c_Payment_ID=p.C_Payment_ID) "
					+ " INNER JOIN C_Invoice i on (psl.C_Invoice_ID = i.C_Invoice_ID)"
					//	Validate if have invoice
					+ "WHERE psc.C_Payment_ID=? AND psl.C_Invoice_ID IS NOT NULL and i.IsPaid = 'N' "
					+ " AND (p.isreceipt = 'N' OR (p.isreceipt = 'Y'))" ;
				PreparedStatement pstmt = null;
				ResultSet rs = null;
				try
				{
					pstmt = DB.prepareStatement(sql, get_TrxName());
					pstmt.setInt(1, payment.getC_Payment_ID());
					rs = pstmt.executeQuery();
					while (rs.next())
					{
						int C_BPartner_ID = rs.getInt(1);
						int C_Invoice_ID = rs.getInt(2);
						if (C_BPartner_ID == 0 && C_Invoice_ID == 0)
							continue;
						boolean isSOTrx = "Y".equals(rs.getString(3));
						BigDecimal PayAmt = rs.getBigDecimal(4);
						BigDecimal DiscountAmt = rs.getBigDecimal(5);
						BigDecimal WriteOffAmt = Env.ZERO;
						BigDecimal OpenAmt = rs.getBigDecimal(7);
						BigDecimal OverUnderAmt = OpenAmt.subtract(PayAmt)
							.subtract(DiscountAmt).subtract(WriteOffAmt);
						//
						if (alloc.get_ID() == 0 && !alloc.save(get_TrxName()))
						{
							log.log(Level.SEVERE, "Could not create Allocation Hdr");
							rs.close();
							pstmt.close();
						}
						MAllocationLine aLine = null;
						if (isSOTrx)
							aLine = new MAllocationLine (alloc, PayAmt, 
								DiscountAmt, WriteOffAmt, OverUnderAmt);
						else
							aLine = new MAllocationLine (alloc, PayAmt.negate(), 
								DiscountAmt.negate(), WriteOffAmt.negate(), OverUnderAmt.negate());
						aLine.setDocInfo(C_BPartner_ID, 0, C_Invoice_ID);
						aLine.setC_Payment_ID(payment.getC_Payment_ID());
						if (!aLine.save(get_TrxName()))
							log.log(Level.SEVERE, "Could not create Allocation Line");
					}
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "allocatePaySelection", e);
				}
				finally
				{
					DB.close(rs, pstmt);
					rs = null;
					pstmt = null;
				}
				
				//	Should start WF
				boolean ok = true;
				if (alloc.get_ID() == 0)
				{
					log.fine("No Allocation created - C_Payment_ID=" 
						+ payment.getC_Payment_ID());
					ok = false;
				}
				else
				{
					alloc.processIt(DocAction.ACTION_Complete);
					ok = alloc.save(get_TrxName());
				}
				payment.testAllocation();
				payment.saveEx();	
			}
			
		});
		return "";
	}
}
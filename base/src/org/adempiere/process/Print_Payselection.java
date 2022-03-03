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

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.model.POWrapper;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.adempiere.webui.window.SimplePDFViewer;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaymentBatch;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.ServerReportCtl;
import org.compiere.print.ReportEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.eevolution.model.MWMInOutBoundLine;

/** Generated Process for (Print_Payselection)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class Print_Payselection extends Print_PayselectionAbstract
{
	private List<File> pdfList = new ArrayList<>();
	private List<MPaySelectionCheck> paySelectionChecks = null;
	private List<MPaySelectionCheck> paySelectionChecksToProcess = null;
	private int paySelectionId = 0;
	private MPaySelection paySelection = null;
	@Override
	protected void prepare()
	{
		super.prepare();
		paySelectionId = getRecord_ID();
		paySelection = new MPaySelection(getCtx(), paySelectionId, get_TrxName());
	}

	@Override
	protected String doIt() throws Exception
	{
		paySelectionChecks = (List<MPaySelectionCheck>) getInstancesForSelection(get_TrxName());
		cmd_print();
		return "";
	}
	
	private void printChecks(){}

private void addPDFFile(File file)
{
	pdfList.add(file);
}
	
	private void cmd_print()
	{
		//	for all checks
		pdfList = new ArrayList<>();
		paySelectionChecks = new ArrayList<MPaySelectionCheck>();
		String sql = "Select distinct paymentRule from C_PayselectionCheck where C_PaySelection_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, paySelectionId);
			rs = pstmt.executeQuery();
			while (rs.next()){
				
				String paymentRule = rs.getString(1);
				log.info(paymentRule);
				if (!getChecks(paymentRule))
					return;
				if (paySelectionChecks.isEmpty())
					continue;

				//	for all checks
				pdfList = new ArrayList<>();
				paySelectionChecksToProcess.stream().filter(paySelectionCheck -> paySelectionCheck != null).forEach(paySelectionCheck -> {
					//	ReportCtrl will check BankAccountDoc for PrintFormat
					ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.CHECK, paySelectionCheck.get_ID());
					try 
					{
						File file = File.createTempFile("WPayPrint", null);
						addPDFFile(re.getPDF(file));
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						return;
					}
				});
				
				SimplePDFViewer chequeViewer = null;
				try 
				{
					File outFile = File.createTempFile("WPayPrint", null);
					AEnv.mergePdf(pdfList, outFile);
					chequeViewer = new SimplePDFViewer("Pay and Print", new FileInputStream(outFile));
					//chequeViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
					//chequeViewer.setWidth("100%");
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					return;
				}

				//	Update BankAccountDoc
				int lastDocumentNo = MPaySelectionCheck.confirmPrint (paySelectionChecksToProcess, null);
				if (lastDocumentNo != 0)
				{
					StringBuffer sb = new StringBuffer();
					sb.append("UPDATE C_BankAccountDoc SET CurrentNext=").append(++lastDocumentNo)
						.append(" WHERE C_BankAccount_ID=").append(paySelection.getC_BankAccount_ID())
						.append(" AND PaymentRule='").append(paymentRule).append("'");
					DB.executeUpdate(sb.toString(), null);
				}

				SimplePDFViewer remitViewer = null; 
				//if (FDialog.ask(0, null, "VPayPrintPrintRemittance"))
				{
					pdfList = new ArrayList<>();
					paySelectionChecks.stream()
							.filter(paySelectionCheck -> paySelectionCheck != null)
							.forEach(paySelectionCheck -> {
						ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.REMITTANCE, paySelectionCheck.get_ID());
						try 
						{
							File file = File.createTempFile("WPayPrint", null);
							addPDFFile(re.getPDF(file));
						}
						catch (Exception e)
						{
							log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					});
					
					try
					{
						File outFile = File.createTempFile("WPayPrint", null);
						AEnv.mergePdf(pdfList, outFile);
						String name = Msg.translate(Env.getCtx(), "Remittance");
						remitViewer = new SimplePDFViewer("Print " + " - " + name, new FileInputStream(outFile));
						//remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
						//remitViewer.setWidth("100%");
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}	//	remittance

				pdfList = new ArrayList<>();
				
				if (chequeViewer != null)
					SessionManager.getAppDesktop().showWindow(chequeViewer);
				
				if (remitViewer != null)
					SessionManager.getAppDesktop().showWindow(remitViewer);
			
				
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
		
	}   //  cmd_print
	
	private boolean getChecks(String paymentRule)
	{
		//  get data
		paySelectionChecksToProcess.clear();
		int startDocumentNo = getCurrentNext();
		AtomicInteger docNo = new AtomicInteger(startDocumentNo);
		log.config("C_PaySelection_ID=" +  ", PaymentRule=" +  paymentRule + ", DocumentNo=" + startDocumentNo);
		//
		//	get Selections
		List<MPaySelectionCheck> localdraftedPayseleChecks = paySelectionChecks;
		localdraftedPayseleChecks.stream()
		.filter(paySelectionCheck -> paySelectionCheck != null
		&& paySelectionCheck.getC_Payment_ID() == 0 
		&& paySelectionCheck.getPaymentRule().equals(paymentRule)).forEach(paySelectionCheck -> {
			paySelectionCheck.setDocumentNo(String.valueOf(docNo.get()));
			docNo.updateAndGet(no -> no + 1);
			paySelectionCheck.saveEx();
			paySelectionChecksToProcess.add(paySelectionCheck);
		});

		//
		//paymentBatch = MPaymentBatch.getForPaySelection (Env.getCtx(), paySelectionId, null);
		return true;
	}   //  getChecks
	
	private int getChecksForReprint(String paymentRule)
	{
		//  get data
		paySelectionChecksToProcess.clear();
		int startDocumentNo = getCurrentNext();
		AtomicInteger docNo = new AtomicInteger(startDocumentNo);
		log.config("C_PaySelection_ID=" +  ", PaymentRule=" +  paymentRule + ", DocumentNo=" + startDocumentNo);
		//
		//	get Selections
		List<MPaySelectionCheck> localdraftedPayseleChecks = paySelectionChecks;
		localdraftedPayseleChecks.stream()
		.filter(paySelectionCheck -> paySelectionCheck != null
		&& paySelectionCheck.getC_Payment_ID() != 0 
		&& paySelectionCheck.getPaymentRule().equals(paymentRule)).forEach(paySelectionCheck -> {
			paySelectionCheck.setDocumentNo(String.valueOf(docNo.get()));
			docNo.updateAndGet(no -> no + 1);
			paySelectionCheck.saveEx();
			paySelectionChecksToProcess.add(paySelectionCheck);
		});

		//
		//paymentBatch = MPaymentBatch.getForPaySelection (Env.getCtx(), paySelectionId, null);
		return docNo.intValue();
	}   //  getChecks
	
	private void cmd_reprint()
	{
		//	for all checks
		pdfList = new ArrayList<>();
		paySelectionChecks = new ArrayList<MPaySelectionCheck>();
		String sql = "Select distinct paymentRule from C_PayselectionCheck where C_PaySelection_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, paySelectionId);
			rs = pstmt.executeQuery();
			while (rs.next()){
				
				String paymentRule = rs.getString(1);
				log.info(paymentRule);
				int lastDocumentNo = getChecksForReprint(paymentRule);
				if (paySelectionChecks.isEmpty())
					continue;

				//	for all checks
				pdfList = new ArrayList<>();
				paySelectionChecksToProcess.stream().filter(paySelectionCheck -> paySelectionCheck != null).forEach(paySelectionCheck -> {
					//	ReportCtrl will check BankAccountDoc for PrintFormat
					ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.CHECK, paySelectionCheck.get_ID());
					try 
					{
						File file = File.createTempFile("WPayPrint", null);
						addPDFFile(re.getPDF(file));
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						return;
					}
				});
				
				SimplePDFViewer chequeViewer = null;
				try 
				{
					File outFile = File.createTempFile("WPayPrint", null);
					AEnv.mergePdf(pdfList, outFile);
					chequeViewer = new SimplePDFViewer("Pay and Print", new FileInputStream(outFile));
					//chequeViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
					//chequeViewer.setWidth("100%");
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					return;
				}

				//	Update BankAccountDoc
				if (lastDocumentNo != 0)
				{
					StringBuffer sb = new StringBuffer();
					sb.append("UPDATE C_BankAccountDoc SET CurrentNext=").append(++lastDocumentNo)
						.append(" WHERE C_BankAccount_ID=").append(paySelection.getC_BankAccount_ID())
						.append(" AND PaymentRule='").append(paymentRule).append("'");
					DB.executeUpdate(sb.toString(), null);
				}

				SimplePDFViewer remitViewer = null; 
				//if (FDialog.ask(0, null, "VPayPrintPrintRemittance"))
				{
					pdfList = new ArrayList<>();
					paySelectionChecks.stream()
							.filter(paySelectionCheck -> paySelectionCheck != null)
							.forEach(paySelectionCheck -> {
						ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.REMITTANCE, paySelectionCheck.get_ID());
						try 
						{
							File file = File.createTempFile("WPayPrint", null);
							addPDFFile(re.getPDF(file));
						}
						catch (Exception e)
						{
							log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					});
					
					try
					{
						File outFile = File.createTempFile("WPayPrint", null);
						AEnv.mergePdf(pdfList, outFile);
						String name = Msg.translate(Env.getCtx(), "Remittance");
						remitViewer = new SimplePDFViewer("Print " + " - " + name, new FileInputStream(outFile));
						//remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
						//remitViewer.setWidth("100%");
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}	//	remittance

				pdfList = new ArrayList<>();
				
				if (chequeViewer != null)
					SessionManager.getAppDesktop().showWindow(chequeViewer);
				
				if (remitViewer != null)
					SessionManager.getAppDesktop().showWindow(remitViewer);
			
				
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
		
	}   //  cmd_print
	
	
	
	



}
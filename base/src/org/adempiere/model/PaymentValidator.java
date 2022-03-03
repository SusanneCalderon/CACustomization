package org.adempiere.model;
/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_PaySelection;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentBatch;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.X_C_Payment;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;


/**
 *	Validator for Customization Central America
 *	
 *  @author Susanne Calderon Systemhaus Westfalia
 */
public class PaymentValidator implements ModelValidator

{
	public PaymentValidator ()
	{
		super ();
	}
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(PaymentValidator.class);
	/** Client			*/
	private int		m_AD_Client_ID = -1;
	
	/**
	 *	Initialize Validation
	 *	@param engine validation engine 
	 *	@param client client
	 */
	public void initialize (ModelValidationEngine engine, MClient client)
	{
		//client = null for global validator
		if (client != null) {	
			m_AD_Client_ID = client.getAD_Client_ID();
			log.info(client.toString());
		}
		else  {
			log.info("Initializing global validator: "+this.toString());
		}

		//	Tables to be monitored
		
		
		engine.addDocValidate(MPaySelection.Table_Name	, this);
		engine.addDocValidate(MBankStatement.Table_Name, this);
		engine.addDocValidate(MPayment.Table_Name, this);
		
		//	Documents to be monitored
	//	engine.addDocValidate(MInvoice.Table_Name, this);

	}	//	initialize

    /**
     *	Model Change of a monitored Table.
     *	Called after PO.beforeSave/PO.beforeDelete
     *	when you called addModelChange for the table
     *	@param po persistent object
     *	@param type TYPE_
     *	@return error message or null
     *	@exception Exception if the recipient wishes the change to be not accept.
     */
	public String modelChange (PO po, int type) throws Exception
	{
		String error = "";
		log.info(po.get_TableName() + " Type: "+type);
		
		if (type == ModelValidator.TYPE_BEFORE_NEW || type == ModelValidator.TYPE_BEFORE_CHANGE ){
			
		}

		if (type == ModelValidator.TYPE_AFTER_CHANGE ){
			
		}
		
		
		
		return error;
	}	//	modelChange
	
	private String PayselectionGeneratePayment(PO po) { 		
		AtomicInteger lastDocumentNo = new AtomicInteger();
		List<MPaySelectionCheck> paySelectionChecks = MPaySelectionCheck.get(po.getCtx(), po.get_ID(), po.get_TrxName());
		paySelectionChecks.stream().filter(psc -> psc != null && !psc.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)).forEach(paySelectionCheck ->
		{
			MPayment payment = new MPayment(paySelectionCheck.getCtx(), paySelectionCheck.getC_Payment_ID(), paySelectionCheck.get_TrxName());
			//	Existing Payment
			if (paySelectionCheck.getC_Payment_ID() != 0
					&& (payment.getDocStatus().equals(DocAction.STATUS_Completed)
								|| payment.getDocStatus().equals(DocAction.STATUS_Closed)))
			{
				//	Update check number
				if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check))
				{
					//payment.setCheckNo(paySelectionCheck.getDocumentNo());
					//payment.saveEx();
				}
			} else {	//	New Payment
				I_C_PaySelection paySelection =  paySelectionCheck.getC_PaySelection();
				MDocType documentType = MDocType.get(paySelectionCheck.getCtx(), paySelection.getC_DocType_ID());
				int docTypeId = documentType.getC_DocTypePayment_ID();
				//	
				payment = new MPayment(paySelectionCheck.getCtx(), 0, paySelectionCheck.get_TrxName());
				payment.setAD_Org_ID(paySelectionCheck.getAD_Org_ID());
				if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)) {
					payment.setBankCheck (paySelectionCheck.getParent().getC_BankAccount_ID(), false, paySelectionCheck.getDocumentNo());
				} else if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_CreditCard)) {
					payment.setTenderType(X_C_Payment.TENDERTYPE_CreditCard);
				} else if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDeposit)
					|| paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDebit)) {
					payment.setBankACH(paySelectionCheck);
				} else {
					//logger.log(Level.SEVERE, "Unsupported Payment Rule=" + paySelectionCheck.getPaymentRule());
					throw  new AdempiereException("Unsupported Payment Rule=" + paySelectionCheck.getPaymentRule());
					//continue;
				}
				payment.setTrxType(X_C_Payment.TRXTYPE_CreditPayment);
				if (docTypeId > 0) {
					payment.setC_DocType_ID(docTypeId);
				}
				payment.setAmount(paySelectionCheck.getParent().getC_Currency_ID(), paySelectionCheck.getPayAmt());
				payment.setDiscountAmt(paySelectionCheck.getDiscountAmt());
				payment.setDateTrx(paySelectionCheck.getParent().getPayDate());
				payment.setDateAcct(payment.getDateTrx()); // globalqss [ 2030685 ]
				payment.setC_BPartner_ID(paySelectionCheck.getC_BPartner_ID());
				List<MPaySelectionLine> paySelectionLines = paySelectionCheck.getPaySelectionLinesAsList(false);
				//logger.fine("confirmPrint - " + paySelectionCheck + " (#SelectionLines=" + (paySelectionLines != null? paySelectionLines.size(): 0) + ")");
				//	For bank Transfer
				if(documentType.isBankTransfer()) {
					payment.setC_Invoice_ID(-1);
					payment.setC_Order_ID(-1);
					//payment.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
					payment.saveEx();
					if(paySelectionLines != null) {
						for(MPaySelectionLine line : paySelectionLines) {
							if (payment.getC_Charge_ID() ==0)
								payment.setC_Charge_ID(line.getC_Charge_ID());
							if(line.getC_BankAccountTo_ID() == 0) {
								throw new AdempiereException("@C_BankAccountTo_ID@ @NotFound@");
							}
							//	For all
							MPayment receiptAccount = new MPayment(paySelectionCheck.getCtx(), 0, paySelectionCheck.get_TrxName());
							PO.copyValues(payment, receiptAccount);
							//	Set default values
							receiptAccount.setC_BankAccount_ID(line.getC_BankAccountTo_ID());
							receiptAccount.setIsReceipt(!payment.isReceipt());
							receiptAccount.setC_DocType_ID(!payment.isReceipt());
							receiptAccount.setRelatedPayment_ID(payment.getC_Payment_ID());
							receiptAccount.setTenderType(payment.getTenderType());
							receiptAccount.setC_Charge_ID(payment.getC_Charge_ID());
							receiptAccount.saveEx();
							receiptAccount.processIt(DocAction.ACTION_Complete);
							receiptAccount.saveEx();
							payment.setRelatedPayment_ID(receiptAccount.getC_Payment_ID());
						}
					}
				} else {
					//	Link to Invoice
					if (paySelectionCheck.getQty() == 1 && paySelectionLines != null && paySelectionLines.size() == 1) {
						MPaySelectionLine paySelectionLine = paySelectionLines.get(0);
						//logger.fine("Map to Invoice " + paySelectionLine);
						//
						//	FR [ 297 ]
						//	For Order
						if(paySelectionLine.getC_Order_ID() != 0) {
							payment.setC_Order_ID (paySelectionLine.getC_Order_ID());
						}
						//	For Charge
						if (paySelectionLine.getC_Charge_ID() != 0) {
							payment.setC_Charge_ID(paySelectionLine.getC_Charge_ID());
							if (paySelectionLine.getHR_Movement_ID() > 0) {
								payment.setC_Project_ID(paySelectionLine.getHRMovement().getC_Project_ID());
							}

						}
						//	For Conversion Type
						if(paySelectionLine.getC_ConversionType_ID() != 0) {
							payment.setC_ConversionType_ID(paySelectionLine.getC_ConversionType_ID());
						}
						//	For Invoice
						if(paySelectionLine.getC_Invoice_ID() != 0) {
							payment.setC_Invoice_ID (paySelectionLine.getC_Invoice_ID());
						}
						//	For all
						payment.setIsPrepayment(paySelectionLine.isPrepayment());
						//	
						payment.setDiscountAmt (paySelectionLine.getDiscountAmt());
						payment.setWriteOffAmt(paySelectionLine.getDifferenceAmt());
						BigDecimal overUnder = paySelectionLine.getOpenAmt().subtract(paySelectionLine.getPayAmt())
							.subtract(paySelectionLine.getDiscountAmt()).subtract(paySelectionLine.getDifferenceAmt());
						payment.setOverUnderAmt(overUnder);
					} else {
						payment.setDiscountAmt(Env.ZERO);
					}
				}
				payment.setWriteOffAmt(Env.ZERO);
				payment.saveEx();
				//	
				paySelectionCheck.setC_Payment_ID (payment.getC_Payment_ID());
				paySelectionCheck.saveEx();	//	Payment process needs it
				//	Should start WF
				payment.processIt(DocAction.ACTION_Complete);
				payment.saveEx();
			}	//	new Payment

			//	Get Check Document No
			try
			{
				int no = Integer.parseInt(paySelectionCheck.getDocumentNo());
				if (lastDocumentNo.get() < no)
					lastDocumentNo.set(no);
			}
			catch (NumberFormatException ex)
			{
				//logger.log(Level.SEVERE, "DocumentNo=" + paySelectionCheck.getDocumentNo(), ex);
			}
			paySelectionCheck.setIsPrinted(false);
			paySelectionCheck.setProcessed(true);
			paySelectionCheck.saveEx();
		});	//	all checks

		//logger.fine("Last Document No = " + lastDocumentNo.get());
		return "";
	}
	

	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("CAValidator");
		return sb.toString ();
	}	//	toString
	

/**
 *	User Login.
	 *	Called when preferences are set
	 *	@param AD_Org_ID org
	 *	@param AD_Role_ID role
	 *	@param AD_User_ID user
	 *	@return error message or null
	 */
	public String login (int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}	//	login

	
	/**
	 *	Get Client to be monitored
	 *	@return AD_Client_ID client
	 */
	public int getAD_Client_ID()
	{
		return m_AD_Client_ID;
	}	//	getAD_Client_ID
	

	public String docValidate (PO po, int timing)
	{
		String error = "";
        if (ModelValidator.TIMING_AFTER_COMPLETE == timing) {
        	if (po instanceof MPaySelection)
        		error = PayselectionGeneratePayment(po);
        	//if (po instanceof MPayment)
        	//	error = PaymentAutoReconcile(po);
        }
        if (ModelValidator.TIMING_AFTER_PREPARE == timing) {
        	if (po instanceof MBankStatement) {
        		error = bs_AfterPrepare(po);
        		error = bs_AfterPrepare_CreatePayment(po);
        	}
        }
        
		return error;
	}	//	docValidate
	

	private String bs_AfterPrepare(PO A_PO)
	{
		Predicate<MBankStatementLine> createPayment = (bsl)->  		
			bsl != null && bsl.get_ValueAsInt(MPayment.COLUMNNAME_C_BankAccount_ID) != 0 && bsl.getC_Payment_ID() ==0;
		
		MBankStatement bankstatement = (MBankStatement)A_PO;
		Arrays.stream(bankstatement.getLines(true))
		.filter(bsl -> createPayment.test(bsl))
		.forEach(bsl -> {
			        MPaymentBatch pBatch = new MPaymentBatch(A_PO.getCtx(), 0 ,  A_PO.get_TrxName());
			        String description = "Transferencia";
			        pBatch.setName("Transferencia");
			        pBatch.saveEx();
			        MPayment paymentBankFrom = new MPayment(A_PO.getCtx(), 0 ,  A_PO.get_TrxName());
			        paymentBankFrom.setC_BankAccount_ID(bankstatement.getC_BankAccount_ID());
			        paymentBankFrom.setC_DocType_ID(false);
			        String value = DB.getDocumentNo(paymentBankFrom.getC_DocType_ID(),A_PO.get_TrxName(), false,  paymentBankFrom);
			        paymentBankFrom.setDocumentNo(value);
			       // paymentBankFrom.setDocumentNo(P_DocumentNo);
			        
			        paymentBankFrom.setDateAcct(bankstatement.getStatementDate());
			        paymentBankFrom.setDateTrx(bankstatement.getStatementDate());
			        paymentBankFrom.setTenderType(X_C_Payment.TENDERTYPE_Account);
			        paymentBankFrom.setDescription(description);
			        paymentBankFrom.setC_BPartner_ID (1000026);
			        paymentBankFrom.setC_Currency_ID(bankstatement.getC_BankAccount().getC_Currency_ID());
			       // if (P_C_ConversionType_ID > 0)
			        paymentBankFrom.setC_ConversionType_ID(114);    
			        paymentBankFrom.setPayAmt(bsl.getTrxAmt().negate());
			        paymentBankFrom.setOverUnderAmt(Env.ZERO);
			        paymentBankFrom.setC_Charge_ID(1000456);
			        paymentBankFrom.setAD_Org_ID(bankstatement.getAD_Org_ID());
			        paymentBankFrom.setC_PaymentBatch_ID(pBatch.getC_PaymentBatch_ID());
			        paymentBankFrom.saveEx();
			        description = description + " desde" +  paymentBankFrom.getC_BankAccount().getAccountNo();
			        paymentBankFrom.processIt(MPayment.DOCACTION_Complete);
			        paymentBankFrom.saveEx();
			        
			        MPayment paymentBankTo = new MPayment(A_PO.getCtx(), 0 ,  A_PO.get_TrxName());
			        paymentBankTo.setC_BankAccount_ID(bsl.get_ValueAsInt("C_BankAccount_ID"));
			        paymentBankTo.setC_DocType_ID(true);
			        value = DB.getDocumentNo(paymentBankTo.getC_DocType_ID(),A_PO.get_TrxName(), false,  paymentBankTo);
			        paymentBankTo.setDocumentNo(value);        
			        paymentBankTo.setC_PaymentBatch_ID(pBatch.getC_PaymentBatch_ID());
			      //  paymentBankTo.setDocumentNo(P_DocumentNo);
			        paymentBankTo.setDateAcct(bankstatement.getStatementDate());
			        paymentBankTo.setDateTrx(bankstatement.getStatementDate());
			        paymentBankTo.setTenderType(X_C_Payment.TENDERTYPE_Account);
			        paymentBankTo.setDescription(description);
			        paymentBankTo.setC_BPartner_ID (1000026);
			        paymentBankTo.setC_Currency_ID(100);        
			        paymentBankFrom.setC_ConversionType_ID(114);    
			        paymentBankTo.setPayAmt(bsl.getTrxAmt().negate());
			        paymentBankTo.setOverUnderAmt(Env.ZERO);
			        paymentBankTo.setC_Charge_ID(1000456);
			        paymentBankTo.setAD_Org_ID(bankstatement.getAD_Org_ID());
			        paymentBankTo.saveEx();
			        description = description + " a " +  paymentBankTo.getC_BankAccount().getAccountNo();
			        paymentBankTo.processIt(MPayment.DOCACTION_Complete);
			        paymentBankTo.saveEx();
			        
			        pBatch.setName(description);        
			        description = description + " Monto:" +  paymentBankTo.getPayAmt();
			        pBatch.set_ValueOfColumn("Description", description);
			        pBatch.setProcessingDate(bankstatement.getStatementDate());
			        pBatch.saveEx();
			        bsl.setPayment(paymentBankFrom);
			        bsl.saveEx();					
				});    
		return "";
	}
	
	private MPayment createPayment (MBankStatementLine bankStatementLine)
		{
			//	Trx Amount = Payment overwrites Statement Amount if defined
			BigDecimal PayAmt = bankStatementLine.getTrxAmt();
			if (PayAmt == null || Env.ZERO.compareTo(PayAmt) == 0)
				PayAmt = bankStatementLine.getStmtAmt();
			if (bankStatementLine.getC_Invoice_ID() == 0
				&& (PayAmt == null || Env.ZERO.compareTo(PayAmt) == 0))
				throw new IllegalStateException ("@PayAmt@ = 0");
			if (PayAmt == null)
				PayAmt = Env.ZERO;
			//
			MPayment payment = new MPayment (bankStatementLine.getCtx(), 0, bankStatementLine.get_TrxName());
			payment.setAD_Org_ID(bankStatementLine.getAD_Org_ID());
			payment.setC_BankAccount_ID(bankStatementLine.getC_BankStatement().getC_BankAccount_ID());
			payment.setTenderType(MPayment.TENDERTYPE_Check);
			if (bankStatementLine.getStatementLineDate()!= null)
				payment.setDateTrx(bankStatementLine.getStatementLineDate());
			else if (bankStatementLine.getStatementLineDate() != null)
				payment.setDateTrx(bankStatementLine.getStatementLineDate());
			if (bankStatementLine.getDateAcct() != null)
				payment.setDateAcct(bankStatementLine.getDateAcct());
			else
				payment.setDateAcct(payment.getDateTrx());
			payment.setDescription(bankStatementLine.getDescription());
			int C_BPartner_ID=0;
			 if (bankStatementLine.getC_BPartner_ID() != 0){
				 C_BPartner_ID = bankStatementLine.getC_BPartner_ID();
			 }
			 else
				 C_BPartner_ID = bankStatementLine.getC_BankStatement().getC_BankAccount().getC_Bank().getC_BPartner_ID();
			 if (C_BPartner_ID !=0)
			{
				payment.setC_BPartner_ID(bankStatementLine.getC_BPartner_ID());
				payment.setC_Currency_ID(bankStatementLine.getC_Currency_ID());
				if (PayAmt.signum() < 0)	//	Payment
				{
					payment.setPayAmt(PayAmt.abs());
					payment.setC_DocType_ID(false);
				}
				else	//	Receipt
				{
					payment.setPayAmt(PayAmt);
					payment.setC_DocType_ID(true);
				}
			}
			else
				return null;
			payment.saveEx();
			//
			payment.processIt(MPayment.DOCACTION_Complete);
			payment.saveEx();
			return payment;		
		}	//	createPayment
	

	private String bs_AfterPrepare_CreatePayment(PO A_PO)
	{
		Predicate<MBankStatementLine> createPayment = (bsl)->  		
			bsl != null && bsl.get_ValueAsInt(MPayment.COLUMNNAME_C_BankAccount_ID) == 0 && bsl.getC_Payment_ID() ==0 && bsl.getC_BPartner_ID() != 0;
		
		MBankStatement bankstatement = (MBankStatement)A_PO;
		Arrays.stream(bankstatement.getLines(true))
		.filter(bsl -> createPayment.test(bsl))
		.forEach(bsl -> {
			MPayment payment = createPayment(bsl);

			if (payment != null) {
				bsl.setC_Payment_ID(payment.getC_Payment_ID());
				bsl.saveEx();				
			}
		});
		return "";
	}
	
}	//	Validator
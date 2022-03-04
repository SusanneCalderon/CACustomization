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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.acct.Doc;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MCharge;
import org.compiere.model.MClient;
import org.compiere.model.MCommissionAmt;
import org.compiere.model.MCommissionDetail;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MElementValue;
import org.compiere.model.MFactAcct;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MProduction;
import org.compiere.model.MProductionLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MProjectLine;
import org.compiere.model.MRequisition;
import org.compiere.model.MTax;
import org.compiere.model.MTaxCategory;
import org.compiere.model.MTaxDeclarationLine;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_Invoice;
import org.compiere.model.X_C_Payment;
import org.compiere.process.DocAction;
import org.compiere.sqlj.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.eevolution.model.MHRAttribute;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.MHREmployee;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MWMInOutBoundLine;
import org.eevolution.model.X_C_TaxDefinition;
import org.globalqss.model.MLCOInvoiceWithholding;


/**
 *	Validator for Customization Central America
 *	
 *  @author Susanne Calderon Systemhaus Westfalia
 */
public class CAValidator implements ModelValidator

{
	public CAValidator ()
	{
		super ();
	}
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(CAValidator.class);
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
		engine.addModelChange(MOrder.Table_Name, this);
		engine.addModelChange(MOrderLine.Table_Name, this);
		engine.addModelChange(MInvoice.Table_Name, this);
		engine.addModelChange(MInvoiceLine.Table_Name, this);
		engine.addModelChange(MCommissionDetail.Table_Name, this);
		engine.addModelChange(MTimeExpenseLine.Table_Name, this);
		engine.addModelChange(MWMInOutBoundLine.Table_Name, this);
		engine.addModelChange(MPaySelectionLine.Table_Name, this);
		engine.addModelChange(MInvoiceTax.Table_Name, this);		
		engine.addModelChange(MPaymentTerm.Table_Name, this);
		engine.addModelChange(MTax.Table_Name, this);
		engine.addModelChange(MTaxCategory.Table_Name, this);
		engine.addModelChange(MUOM.Table_Name, this);
		engine.addModelChange(MProduct.Table_Name, this);
		engine.addModelChange(MCharge.Table_Name, this);
		engine.addModelChange(MElementValue.Table_Name, this);
		engine.addModelChange(MDocType.Table_Name, this);
		engine.addModelChange(MAllocationHdr.Table_Name, this);
		engine.addModelChange(MProductPO.Table_Name, this);
		engine.addModelChange(MCostDetail.Table_Name, this);
		engine.addModelChange(MPaySelectionLine.Table_Name, this);
		

		engine.addDocValidate(MPaySelection.Table_Name	, this);
		engine.addDocValidate(MBankStatement.Table_Name, this);
		engine.addDocValidate(MOrder.Table_Name, this);
		engine.addDocValidate(MPayment.Table_Name, this);
		engine.addDocValidate(MTimeExpense.Table_Name, this);
		engine.addDocValidate(MMovement.Table_Name, this);
		engine.addDocValidate(MInventory.Table_Name, this);
		engine.addDocValidate(MProjectIssue.Table_Name, this);
		engine.addDocValidate(MProduction.Table_Name, this);
		engine.addDocValidate(MAllocationHdr.Table_Name, this);
		engine.addDocValidate(MInOut.Table_Name, this);
		engine.addDocValidate(MInvoice.Table_Name, this);
		engine.addDocValidate(MDDOrder.Table_Name, this);
		
		
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
			if (po.get_TableName().equals(MOrder.Table_Name)) {
				error = User4Mandatory(po);
				if (po.is_ValueChanged(MOrder.COLUMNNAME_C_DocTypeTarget_ID) || po.is_ValueChanged(MOrder.COLUMNNAME_M_PriceList_ID))
					error = controlPriceListDocType(po);
					;
				
			}
			if(po.get_TableName().equals(MInvoice.Table_Name)) {
}
			if (po.get_TableName().equals(MTimeExpenseLine.Table_Name))
				error = calculateLabourCost(po);
			if (po.get_TableName().equals(MOrderLine.Table_Name)
					&& (po.is_ValueChanged("M_Product_ID") || po.is_ValueChanged("C_Charge_ID")))
				error = setOrderLineTax(po);
			if (po.get_TableName().equals(MOrderLine.Table_Name))
				error = updatePriceLimitControl(po);
			

			if (po.get_TableName().equals(MInvoiceLine.Table_Name)
					&& (po.is_ValueChanged("M_Product_ID") || po.is_ValueChanged("C_Charge_ID")))
			{
				error = setInvoiceLineTax(po);
				error = UpdateCreditMemoLine(po);
			}
			 if (po.get_TableName().equals(MWMInOutBoundLine.Table_Name)) {
				 error = updateM_Locator_TO(po);					 
			 }
			//if (po.get_TableName().equals(MTaxDeclarationLine.Table_Name) && type == ModelValidator.TYPE_BEFORE_NEW)
			//	error = updateTaxDeclarationLIne(po);
			 if (po.get_TableName().equals(MPayment.Table_Name)) {
				 error = UpdatePaymentWithholing(po);
			 }
			 //if (po instanceof MInvoiceTax) {
			//	 error = controlInvoiceTax(po);
			 //}
			 
			 if (po instanceof MProductPO) {
				 error = productPOCheckCurrentVendor(po);
			 }
			 
		}

		 if (po instanceof MOrderLine) 
		 {
			 if (type == ModelValidator.TYPE_AFTER_NEW) {
				 error = updateCreditLimitControl(po);
			 }
			 else if (type == ModelValidator.TYPE_AFTER_CHANGE)				 
			 {
				 if ( (po.is_ValueChanged(MOrderLine.COLUMNNAME_M_Product_ID)
						 || po.is_ValueChanged(MOrderLine.COLUMNNAME_PriceEntered)
						 ||po.is_ValueChanged(MOrderLine.COLUMNNAME_QtyEntered))) {
					 error = updateCreditLimitControl(po);
				 }			 
			 }		 
		 }

		if (type == ModelValidator.TYPE_AFTER_CHANGE ){
			if (po.get_TableName().equals(MOrder.Table_Name))
				error = updateOrderLines(po);
			if(po.get_TableName().equals(MInvoice.Table_Name)) {
				error = updateInvoiceLines(po);	
				
			}
			if (po.get_TableName().equals(MCommissionDetail.Table_Name)) {
				error = commissionAmtUpdate(po);
			}
			if (po instanceof MProduct || po instanceof MCharge || po instanceof MDocType || po instanceof MUOM
					|| po instanceof MElementValue || po instanceof MTax || po instanceof MTaxCategory || po instanceof MPaymentTerm) {
				error = updateTranslation(po);
			}
			
			if (po instanceof MDDOrderLine
					&& (TYPE_AFTER_CHANGE == type && po.is_ValueChanged(MDDOrderLine.COLUMNNAME_QtyDelivered)))
				//error = updateWMInOutBoundLine(po)
				;
			
		}
		if (type == ModelValidator.TYPE_BEFORE_NEW ) {
			if( po.get_TableName().equals(MOrder.Table_Name)) {
				error = UpdatePaymentRule(po);
				if (po.get_TableName().equals(MPaySelectionLine.Table_Name)) {
					error = C_PayselectionLine_GetPaymentRuleFromHeader(po);						
				}
				if (po.is_ValueChanged(MOrder.COLUMNNAME_DateOrdered))
					UpdateDatePromised(po);
			}
			if (po instanceof MPaySelectionLine)
				error = paySelectionLineUpdatebpartnerName(po);
		}
		
		if (type == ModelValidator.TYPE_BEFORE_CHANGE && po.get_TableName().equals(MProductionLine.Table_Name))
			if (po.is_ValueChanged(MProductionLine.COLUMNNAME_QtyUsed)){
			error = productionLineUpdateMovementQtyFromQtyUsed(po);
		}
		
		if (type == ModelValidator.TYPE_BEFORE_DELETE) {
			if (po instanceof MAllocationHdr)
				error = allocationHdrBeforeDelete(po);
		}
		
		if (type == ModelValidator.TYPE_BEFORE_CHANGE || type == ModelValidator.TYPE_BEFORE_NEW) {
			if ((po.get_ColumnIndex("C_BPartner_ID") > 0 && po.is_ValueChanged("C_BPartner_ID")) 
				&& po.get_ColumnIndex(X_C_Invoice.COLUMNNAME_User2_ID) > 0) {
				int C_BPartner_ID = po.get_ValueAsInt("C_BPartner_ID");
				ArrayList<Object> params = new ArrayList<>();
				params.add(C_BPartner_ID);
				params.add(Env.getAD_Client_ID(po.getCtx()));
				BigDecimal User2_ID = DB.getSQLValueBD(po.get_TrxName(), 
						"select getUser2_ID(?,?) ",
						params);
				po.set_ValueOfColumn(X_C_Invoice.COLUMNNAME_User2_ID, User2_ID);
			}
			if ((po.get_ColumnIndex("M_Product_ID") > 0
					&& po.is_ValueChanged("M_Product_ID")) && po.get_ColumnIndex(X_C_Invoice.COLUMNNAME_User1_ID) > 0) {
				int M_Product_ID = po.get_ValueAsInt("M_Product_ID");
				ArrayList<Object> params = new ArrayList<>();
				params.add(M_Product_ID);
				params.add(Env.getAD_Client_ID(po.getCtx()));
				BigDecimal User1_ID = DB.getSQLValueBD(po.get_TrxName(), 
						"select getUser1_ID(?,?) ", params);
				po.set_ValueOfColumn(X_C_Invoice.COLUMNNAME_User1_ID, User1_ID);
				BigDecimal User3_ID = DB.getSQLValueBD(po.get_TrxName(), 
						"Select getUser3_ID(?,?) ",params);
				po.set_ValueOfColumn(X_C_Invoice.COLUMNNAME_User3_ID, User3_ID);
			}


		}
		return error;
	}

	/**
	 * 
	 */private String productionLineUpdateMovementQtyFromQtyUsed(PO po){
		 MProductionLine productionLine = (MProductionLine)po;
			if ( productionLine.getM_Product().isBOM() && productionLine.getM_Product().isStocked() )
			{
				productionLine.setMovementQty(productionLine.getQtyUsed().negate());
			}
		 return "";
	 }	//	modelChange

	/**
	 * 
	 */private String C_PayselectionLine_GetPaymentRuleFromHeader(PO po){
		 
		 MPaySelectionLine paySelectionLine =  (MPaySelectionLine)po;
		 MPaySelection paySelection = paySelectionLine.getParent();

		 Boolean isSoTrx = paySelection.get_ValueAsBoolean("IsSOTrx");
		 if (!isSoTrx)
			 return "";
		 paySelectionLine.setPaymentRule(paySelection.get_ValueAsString(MPaySelectionLine.COLUMNNAME_PaymentRule));
		 return "";
	 }
	 

	private String C_PayselectionLine_calculateWithHolding(PO po){
		 
		 MPaySelectionLine paySelectionLine =  (MPaySelectionLine)po;
		 BigDecimal sumwhamt = Env.ZERO;
		 String whereClause = "C_Invoice_ID = ? AND IsActive = 'Y' AND " +
					"IsCalcOnPayment = 'Y' AND Processed = 'N' AND " +
					"C_AllocationLine_ID IS NULL";
		 List<MLCOInvoiceWithholding> iwhs = new Query(paySelectionLine.getCtx(), MLCOInvoiceWithholding.Table_Name, 
				 whereClause, paySelectionLine.get_TrxName())
					.setParameters(paySelectionLine.getC_Invoice_ID())
					.setOnlyActiveRecords(true)
					.list();
		 for (MLCOInvoiceWithholding iwh:iwhs)
			{						
				sumwhamt = sumwhamt.add(iwh.getTaxAmt());
			}
		 if (sumwhamt.compareTo(Env.ZERO) !=0) {
			 paySelectionLine.setPayAmt(paySelectionLine.getPayAmt().subtract(sumwhamt));
			 paySelectionLine.saveEx();
		 }
		 return "";
	 }

	private String User4Mandatory(PO po) {
		MOrder order = (MOrder)po;
		if(order.getC_DocTypeTarget().isHasCharges() && order.getUser4_ID() <=0)
		{
			return "EL campo retaceo es obligatorio";
		}
		return "";
	}
	

	private String updateM_Locator_TO(PO po) {
		MWMInOutBoundLine inOutBoundLine = (MWMInOutBoundLine)po;
		if(inOutBoundLine.getM_LocatorTo_ID() <=0)
			inOutBoundLine.setM_LocatorTo_ID(inOutBoundLine.getWM_InOutBound().getM_Locator_ID());
		
		return "";
	}
	
	private String UpdatePaymentRule(PO po) {
		MOrder order = (MOrder)po;
		if(order.getC_DocTypeTarget().getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_POSOrder))
		{
			order.setPaymentRule(MOrder.PAYMENTRULE_Cash);
		}
		return "";
	}
	
	private String UpdateDatePromised(PO po) {
		MOrder order = (MOrder)po;
		if (!order.isSOTrx())
			return "";
		Timestamp datePromised = order.getDateOrdered();
		try {
			GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
				cal.setTimeInMillis(datePromised.getTime());
				cal.add(Calendar.DAY_OF_YEAR, +3);	//	next
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				datePromised =  new Timestamp (cal.getTimeInMillis());
				datePromised = nextBusinessDay(datePromised, order.get_TrxName());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		order.setDatePromised(datePromised);
		return "";
	}
	
	
	private String updateOrderLines(PO po) {   
		MOrder order = (MOrder)po;
		if(order.is_ValueChanged("User4_ID"))
		{
			for(MOrderLine orderLine:order.getLines()) 
			{
				orderLine.setUser4_ID(order.getUser4_ID());
				orderLine.saveEx();            
			}
		}

		if(order.is_ValueChanged(MOrder.COLUMNNAME_C_Project_ID))
		{
			for(MOrderLine orderLine:order.getLines()) 
			{
				orderLine.setC_Project_ID(order.getC_Project_ID());;
				orderLine.saveEx();            
			}
		}
		
		return "";
	}
	
	private String beforeCompleteIsControlLimitPrice(PO po) {   
		String result = "";
		MOrder order = (MOrder)po;
		if (!order.isSOTrx())
			return result;
		if (!order.get_ValueAsBoolean("isControlLimitPrice")) {			
			return "";
		}
		StringBuffer sqlRole = new StringBuffer();
		sqlRole.append("SELECT count(*) FROM AD_Role r  WHERE r.IsActive='Y' ");
		sqlRole.append("	AND EXISTS (SELECT * FROM AD_User_Roles ur");
		sqlRole.append("	WHERE r.AD_Role_ID=ur.AD_Role_ID  AND ur.AD_User_ID=?) ");
		sqlRole.append("	AND r.IsDiscountUptoLimitPrice = 'N' ");
		int noRole = DB.getSQLValueEx(order.get_TrxName(), sqlRole.toString(), Env.getAD_User_ID(order.getCtx()));
		if (noRole>0) {
			order.set_ValueOfColumn("isControlLimitPrice", false);
			order.set_ValueOfColumn("limitPriceApprovedBy", Env.getAD_User_ID(order.getCtx()));
			order.saveEx();
			return result;			
		}
		else {

			result = "Excede Limite de Precio";		
			}
		return result;
	}
	
	private String updateCreditLimitControl(PO po) {	
		MOrderLine orderLine = (MOrderLine)po;
		if (orderLine.getParent().isSOTrx()) {
			String sql = "";
			int no = 0;
			ArrayList<Object> params = new ArrayList<Object>();
			BigDecimal creditLimit = orderLine.getParent().getC_BPartner().getSO_CreditLimit();
			BigDecimal openBalance  = orderLine.getParent().getC_BPartner().getTotalOpenBalance();
			if (openBalance.compareTo(Env.ZERO) <= 0)
				return "";
			String rejectStatus = (creditLimit.compareTo(orderLine.getParent().getGrandTotal().add(openBalance)) < 0)
					&& (creditLimit.compareTo(Env.ZERO)) > 0?"CL":"NO";			
					params.add(rejectStatus);
					sql = "UPDATE C_Order i "
							+ " SET DocStatus_RejectStatus= ?"
							+ " WHERE C_Order_ID=" + orderLine.getC_Order_ID();				
					no = DB.executeUpdateEx(sql, params.toArray(), po.get_TrxName());
					if (!rejectStatus.equals("NO"))
						return "";
					sql = "SELECT 1 "
							+ "FROM C_Invoice i "
							+ "LEFT JOIN C_PaymentTerm pt ON(pt.C_PaymentTerm_ID = i.C_PaymentTerm_ID) "
							+ "WHERE i.IsSotrx = 'Y' "
							+ "AND i.C_BPartner_ID=? "
							+ "AND i.IsPaid = 'N' "
							+ "AND (paymenttermDueDays(i.C_PaymentTerm_ID, i.DateInvoiced, getDate()) - pt.GraceDays) > 0";
					int noInvoicesDue = DB.getSQLValueEx(null, sql, orderLine.getParent().getC_BPartner_ID());
					if (noInvoicesDue>0) {
						sql = "UPDATE C_Order i "
								+ " SET DocStatus_RejectStatus='DI' "
								+ "WHERE C_Order_ID=" + orderLine.getC_Order_ID();
						no = DB.executeUpdate(sql, po.get_TrxName());			
					}
		}
		return "";
	}
	
	private String updatePriceLimitControl(PO po) {	
		MOrderLine orderLine = (MOrderLine)po;
		if (!orderLine.getParent().isSOTrx())
			return "";	
		if(orderLine.is_ValueChanged(MOrderLine.COLUMNNAME_PriceEntered)) {
		Boolean controlPriceLimit =	orderLine.getPriceActual().compareTo(orderLine.getPriceLimit())< 0?true:false;			
				orderLine.set_ValueOfColumn("isControlLimitPrice", controlPriceLimit);
				orderLine.getParent().set_ValueOfColumn("isControlLimitPrice", controlPriceLimit);
				orderLine.getParent().saveEx();		
							
		}
		return "";
	}
	
	
	private String beforeCompleteOrderControlCreditStop(PO po) {  
		MOrder order = (MOrder)po;
		String result = "";
		BigDecimal newAmt = order.getC_BPartner().getTotalOpenBalance().add(order.getGrandTotal());
		BigDecimal creditLimit = order.getC_BPartner().getSO_CreditLimit();
		if (!order.isSOTrx() 
        		|| order.getC_BPartner().getSO_CreditLimit() == Env.ZERO
        		|| order.getC_BPartner().getTotalOpenBalance().compareTo(Env.ZERO) <=0)
			return result;
		BigDecimal additionalAmt = order.getGrandTotal();
		StringBuffer sqlRole = new StringBuffer();
		sqlRole.append("SELECT count(*) FROM AD_Role r  WHERE r.IsActive='Y' ");
		sqlRole.append("	AND EXISTS (SELECT * FROM AD_User_Roles ur");
		sqlRole.append("	WHERE r.AD_Role_ID=ur.AD_Role_ID AND ur.AD_User_ID=?) ");
		sqlRole.append("	AND r.IsCanApproveCreditLimit = 'Y' ");
		int noRole = DB.getSQLValueEx(order.get_TrxName(), sqlRole.toString(), Env.getAD_User_ID(order.getCtx()));
		if (noRole>0) {
			//order.set_ValueOfColumn("DocStatus_RejectStatus", "NO");
			order.set_ValueOfColumn("creditApprovedBy", Env.getAD_User_ID(order.getCtx()));
			order.saveEx();
			return result;			
		}
		
		if (additionalAmt == null || additionalAmt.signum() == 0)
			return "";
		//
		//	Nothing to do
		if (creditLimit.compareTo(newAmt) < 0) {
			return "Venta exede Limite de Credito";
		}
		//	Above (reduced) Credit Limit
		
			int noInvoicesDue = 0;
			StringBuffer sql = new StringBuffer("SELECT 1 "
					+ "FROM C_Invoice i "
					+ "LEFT JOIN C_PaymentTerm pt ON(pt.C_PaymentTerm_ID = i.C_PaymentTerm_ID) "
					+ "WHERE i.IsSotrx = 'Y' "
					+ "AND i.C_BPartner_ID=? "
					+ "AND i.IsPaid = 'N' "
					+ "AND (paymenttermDueDays(i.C_PaymentTerm_ID, i.DateInvoiced, getDate()) - pt.GraceDays) > 0");
			if(order.getC_BPartner().getDunningGrace() != null) {
				sql.append(" AND paymenttermDueDate(i.C_PaymentTerm_ID, i.DateInvoiced) >= ?");
				noInvoicesDue = DB.getSQLValueEx(null, sql.toString(), order.getC_BPartner_ID(), order.getC_BPartner().getDunningGrace());
			} else {
				noInvoicesDue = DB.getSQLValueEx(null, sql.toString(), order.getC_BPartner_ID());
			}
			if (noInvoicesDue == 1) {
				order.set_ValueOfColumn("DocStatus_RejectStatus", "DI");
				order.saveEx();
				return "Cliente tiene facturas pendientes";
			}

		//	is OK
		order.saveEx();
		return "";
	
	}
	
	private String upDateControlCredito(PO po) {
		
		MOrder order = (MOrder)po;
		if (!order.isSOTrx())
			return "";
		if(order.is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_ID)) {
			String sql = "select count(*) from rv_OpenItem oi " + 
					" INNER JOIN C_Paymentterm pt on oi.c_Paymentterm_ID=pt.c_Paymentterm_ID " + 
					" WHERE IsSotrx = 'Y' and C_BPartner_ID=? " + 
					" AND daysDue > pt.netdays";
			int noInvoicesDue = DB.getSQLValueEx(order.get_TrxName(), sql, order.getC_BPartner_ID());
			order.set_ValueOfColumn("ISpastDueInvoices", noInvoicesDue>0?true:false);
			if (noInvoicesDue>0){
				order.set_ValueOfColumn("DocStatus_RejectStatus", "CL");
			}
		}
		if(order.is_ValueChanged(MOrder.COLUMNNAME_DateOrdered)) {
			if ( (order.getC_DocTypeTarget().getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_StandardOrder)
				|| order.getC_DocTypeTarget().getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Proposal)
				|| order.getC_DocTypeTarget().getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Quotation)))
				return"";
			Timestamp datePromised = order.getDatePromised();
			GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
				cal.setTimeInMillis(datePromised.getTime());
				cal.add(Calendar.DAY_OF_YEAR, +3);	//	next
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			try {
				datePromised = Adempiere.nextBusinessDay(datePromised);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			order.setDatePromised(datePromised);
		}
		return "";
	}
	
	


	private String updateTaxDeclarationLIne(PO po) {
		MTaxDeclarationLine declarationLine = (MTaxDeclarationLine)po;
		if (declarationLine.getC_Invoice().getC_DocType_ID() != 1000455)
			return "";
		String sql = "select ivl.linenetamt from c_Invoiceline ivl inner join c_INvoice i on ivl.c_Invoice_ID=i.c_Invoice_ID "
				+ "	where docstatus in ('CO','CL') and ivl.ref_Invoice_ID =?";
		BigDecimal taxamt = DB.getSQLValueBDEx(declarationLine.get_TrxName(), sql, declarationLine.getC_Invoice_ID());
		declarationLine.setTaxAmt(taxamt);
		
		return "";
	}
	

	private String InvoiceTypeTaxDeclaration(PO po) {
		MInvoice invoice = (MInvoice)po;
		if (invoice.isSOTrx())
			return"";
		if(invoice.getC_DocTypeTarget().isHasCharges() && invoice.getUser4_ID() <=0)
		{
			return "EL campo retaceo es obligatorio";
		}
		MDocType docType = (MDocType)invoice.getC_DocTypeTarget();
		if(docType.get_ValueAsString("invoiceType").equals("ICD"))
		{
			if (invoice.get_ValueAsInt("Ref_BPartner_ID")<=0)
				return "@No Reference Vendor";
			BigDecimal amount = (BigDecimal)invoice.get_Value("ActualAmt");
			BigDecimal taxaAmt = (BigDecimal)invoice.get_Value("TaxAmt");
			if(amount.longValue()<=0 || taxaAmt.longValue()<=0)
				return "@Invoice Amount and Tax Amount are mandatory";
		}
		return "";
	}
	
	
	private String updateInvoiceLines(PO po) { 
		MInvoice invoice = (MInvoice)po;
		if(invoice.is_ValueChanged("User4_ID")) 
		{
			for(MInvoiceLine invoiceLine:invoice.getLines()) 
			{
				invoiceLine.setUser4_ID(invoice.getUser4_ID());
				invoiceLine.saveEx();            
			}

			if(invoice.is_ValueChanged(MInvoice.COLUMNNAME_C_Project_ID))
			{
				for(MInvoiceLine invoiceLine:invoice.getLines()) 
				{
					invoiceLine.setC_Project_ID(invoice.getC_Project_ID());
					invoiceLine.saveEx();            
				}
			}
		}
		return "";
	}
	

	private String calculateLabourCost(PO A_PO) {
		Boolean changed = false;
		MTimeExpenseLine expenseLine = (MTimeExpenseLine)A_PO;
		if(expenseLine.is_ValueChanged("FeeAmt") || 
				expenseLine.is_ValueChanged("TravelCost") ||
				expenseLine.is_ValueChanged("HR_Concept_ID") ||
				expenseLine.is_ValueChanged(MTimeExpenseLine.COLUMNNAME_Qty) ||
				expenseLine.is_ValueChanged(MTimeExpenseLine.COLUMNNAME_Description)
				)
			changed = true;
		if (!changed)
			return "";
		MHRConcept conceptProduct = new MHRConcept(expenseLine.getCtx(), expenseLine.get_ValueAsInt(MHRConcept.COLUMNNAME_HR_Concept_ID), expenseLine.get_TrxName());
		MHRAttribute productattribute = MHRAttribute.getByConceptAndEmployee(conceptProduct, null, 0, expenseLine.getParent().getDateReport(), expenseLine.getParent().getDateReport());
		BigDecimal factor = productattribute.getAmount();
		
		MHRConcept salaryConcept = MHRConcept.getByValue(expenseLine.getCtx(), "Salarios", expenseLine.get_TrxName());
		MHREmployee employee = MHREmployee.getActiveEmployee(A_PO.getCtx(), expenseLine.getC_BPartner_ID(), A_PO.get_TrxName());
		if (employee == null) {
			employee = new Query(expenseLine.getCtx(), MHREmployee.Table_Name, "C_BPartner_ID=?", expenseLine.get_TrxName())
					.setParameters(expenseLine.getC_BPartner_ID())
					.setOrderBy("HR_Employee_ID desc ")
					.first();
			if (employee == null)
				return expenseLine.getC_BPartner().getName() + " no esta en planilla";
			}
		if (employee.getHR_Payroll_ID() == 0)
			return expenseLine.getC_BPartner().getName() + " sin contrato";
		MHRAttribute salaryAttribute = MHRAttribute.getByConceptAndEmployee(salaryConcept, employee, employee.getHR_Payroll_ID(), 
				expenseLine.getParent().getDateReport(), expenseLine.getParent().getDateReport());
		
		if (salaryAttribute == null || salaryAttribute.getAmount().signum()==0)
			return expenseLine.getC_BPartner().getName() + " Falta definir el sueldo básico para este empleado";
		int precision = salaryConcept.getStdPrecision() > 0? salaryConcept.getStdPrecision()
				: expenseLine.getC_Currency().getStdPrecision();		
		BigDecimal salaryhour = salaryAttribute.getAmount().divide(new BigDecimal(30.00), precision, BigDecimal.ROUND_HALF_UP);
		
		salaryhour = salaryhour.divide(new BigDecimal(8.00), precision, BigDecimal.ROUND_HALF_UP);
		salaryhour = salaryhour.multiply(factor);
		BigDecimal costtotal = salaryhour.multiply(expenseLine.getQty());
		expenseLine.setExpenseAmt(salaryhour);

		if (expenseLine.getC_Currency_ID() == expenseLine.getC_Currency_Report_ID())
			expenseLine.setConvertedAmt(costtotal);
		else
		{
			expenseLine.setConvertedAmt(MConversionRate.convert (expenseLine.getCtx(),
					costtotal, expenseLine.getC_Currency_ID(), expenseLine.getC_Currency_Report_ID(), 
					expenseLine.getDateExpense(), 0, getAD_Client_ID(), expenseLine.getAD_Org_ID()) );
		}
		BigDecimal feeAmt = (BigDecimal)expenseLine.get_Value("FeeAmt");
		BigDecimal travelCost =  (BigDecimal)expenseLine.get_Value("TravelCost");
		BigDecimal prepaymentAmt =  (BigDecimal)expenseLine.get_Value("PrepaymentAmt");
		expenseLine.setQtyReimbursed(Env.ONE);
		expenseLine.setPriceReimbursed(feeAmt.add(travelCost).subtract(prepaymentAmt));
		if (!expenseLine.getParent().getDocStatus().equals("CO"))		
			return "";
		String sqlUpdate = 
				"update c_ProjectLine pl  set committedamt = (select coalesce(convertedamt,0) + coalesce(pricereimbursed,0) from s_timeexpenseline where s_Timeexpenseline_ID=pl.s_Timeexpenseline_ID)" + 
				" where pl.s_timeexpenseline_ID = " + expenseLine.getS_TimeExpenseLine_ID();
		int no = DB.executeUpdateEx(sqlUpdate,  expenseLine.get_TrxName());
		
		return "";
	}
	
	private String PayselectionGeneratePayment(PO po) { 
		
		AtomicInteger lastDocumentNo = new AtomicInteger();
		List<MPaySelectionCheck> paySelectionChecks = MPaySelectionCheck.get(po.getCtx(), po.get_ID(), po.get_TrxName());
		//paySelectionChecks.stream().filter(psc -> psc != null && 
			//	!psc.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check))
		//.forEach(paySelectionCheck ->
		for (MPaySelectionCheck paySelectionCheck:paySelectionChecks)
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
					payment.setCheckNo(paySelectionCheck.getParent().get_ValueAsString("CheckNo"));
					payment.saveEx();
				}
				else if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDeposit))
				{
					payment.setR_AuthCode(paySelectionCheck.getParent().get_ValueAsString("R_AuthCode"));
					payment.saveEx();
				}
			} else {	//	New Payment
				MPaySelection paySelection =  paySelectionCheck.getParent();

				Boolean payInmediate = true;
				if (paySelection.getC_DocType().isSOTrx()) {
					//String TransactionCode = paySelectionCheck.get_ValueAsString("TransactionCode");
					//if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check) && TransactionCode.length()>0)
						payInmediate = true;
				}
				else {
					if ((paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check) ) ||
							paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDeposit))
						payInmediate = false;
				}
				if (!payInmediate)
					continue;
				MDocType documentType = MDocType.get(paySelectionCheck.getCtx(), paySelection.getC_DocType_ID());
				int docTypeId = documentType.getC_DocTypePayment_ID();
				//	
				payment = new MPayment(paySelectionCheck.getCtx(), 0, paySelectionCheck.get_TrxName());
				payment.setAD_Org_ID(paySelectionCheck.getAD_Org_ID());
				if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)
						|| paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Cash)) {
					payment.setBankCheck (paySelectionCheck.getParent().getC_BankAccount_ID(), false, paySelectionCheck.getDocumentNo());
					payment.setCheckNo(paySelection.get_ValueAsString("CheckNo"));
					payment.setVoiceAuthCode(paySelectionCheck.get_ValueAsString("TransactionCode"));
					if (paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Cash))
						payment.setTenderType(MPayment.TENDERTYPE_Cash);
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
			paySelectionCheck.setIsPrinted(true);
			paySelectionCheck.setProcessed(true);
			paySelectionCheck.saveEx();
		}
		//);	//	all checks

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
		
		  StringBuffer sqlRole = new StringBuffer();
		  sqlRole.append("SELECT count(*) FROM AD_Role r  WHERE r.IsActive='Y' ");
		  sqlRole.append("	AND EXISTS (SELECT * FROM AD_User_Roles ur"); 
		  sqlRole.append("	WHERE r.AD_Role_ID=ur.AD_Role_ID AND ur.IsActive='Y' AND ur.AD_User_ID= " + AD_User_ID); 
		  sqlRole.append("	AND r.iscanapprovecreditlimit = 'Y' )"); 
		  int noRole = DB.getSQLValueEx(null, sqlRole.toString());
		  if (noRole>0) { Env.setContext(Env.getCtx(), "#CanApproveCreditlimit", true);
		  } else Env.setContext(Env.getCtx(), "#CanApproveCreditlimit", false);
		  
		  StringBuffer sqlRolePriceLimit = new StringBuffer(); 
		  sqlRolePriceLimit.append("SELECT count(*) FROM AD_Role r  WHERE r.IsActive='Y' ");
		  sqlRolePriceLimit.append("	AND EXISTS (SELECT * FROM AD_User_Roles ur");
		  sqlRolePriceLimit. append("	WHERE r.AD_Role_ID=ur.AD_Role_ID AND ur.IsActive='Y' AND ur.AD_User_ID= " + AD_User_ID); 
		  sqlRolePriceLimit.append("	AND r.OverwritePriceLimit = 'Y') "); 
		  noRole = DB.getSQLValueEx(null, sqlRolePriceLimit.toString()); 
		  if (noRole>0) {
		  Env.setContext(Env.getCtx(), "#CanApprovePricelimit", true); 
		  } 
		  else
		  Env.setContext(Env.getCtx(), "#CanApprovePricelimit", false);
		 
		
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
        	//if (po instanceof MPaySelection)
        	//	error = PayselectionGeneratePayment(po);
        	if (po instanceof MPayment)
        		error = PaymentAutoReconcile(po);
        	if (po instanceof MTimeExpense)
        		error = TimeExpenseReportCreateProjectIssue(po);
        	if (po instanceof MOrder)
        		error = updateUser4Order(po);
        	if (po instanceof MInOut)
        		error = updateUser4MaterialReceipt(po);
        	if (po instanceof MMovement)
        		error = updateDDOrderLine(po);
        }
        if (ModelValidator.TIMING_AFTER_PREPARE == timing) {
        	
        	if (po instanceof MOrder) {
        		error = OrderSetPrecision(po);
        	}
        	if (po instanceof MInvoice) {
        		error = InvoiceSetPrecision(po);
        	}
        }
        if (ModelValidator.TIMING_BEFORE_POST == timing) {
        	if (po.get_ColumnIndex("Posted") > 0)        		
        	//error = factAcct_UpdateDocumentNO(po)
        		;
        }
        if (ModelValidator.DOCTIMING_BEFORE_PREPARE == timing) {
        	if (po instanceof MInvoice) {

				error = UpdateCreditMemo(po);
        	}   	
        	if (po instanceof MMovement) {
        		error = movementBeforePrepare(po);        		
        	}
        	if (po instanceof MInOut) {
        		error = inoutLineSetOrderLine(po);
        	}
;        }
        if (ModelValidator.TIMING_BEFORE_VOID == timing) {
        	if (po instanceof MPaySelection)
        		error = voidIncludedPayments(po);
        }
        
        if (ModelValidator.TIMING_BEFORE_COMPLETE == timing) {
        	if (po instanceof MOrder) {
        		error = testQtyOnhand(po);
        		if (!error.equals("")) {
        			return error;
        		}	
        	    error = beforeCompleteIsControlLimitPrice(po);
        	    if (!error.equals("")) {
        			return error;
        		}	
                error = beforeCompleteOrderControlCreditStop(po);
        	}
        }
        
		return error;
	}

	/**
	 * @param po
	 */
	private String voidIncludedPayments(PO po) {
		List<MPaySelectionCheck> paySelectionChecks = MPaySelectionCheck.get(po.getCtx(), po.get_ID(), po.get_TrxName());
		for (MPaySelectionCheck paySelectionCheck:paySelectionChecks) {
			MPayment payment = (MPayment)paySelectionCheck.getC_Payment();
			try
			{
				payment.voidIt();
				payment.saveEx();
			}
			catch (NumberFormatException ex)
			{
				//logger.log(Level.SEVERE, "DocumentNo=" + paySelectionCheck.getDocumentNo(), ex);
			}
		}
		return "";
	}	//	docValidate
	

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
	

	private String commissionAmtUpdate(PO A_PO)
	{
		MCommissionDetail commissionDetail = (MCommissionDetail)A_PO;
		MCommissionAmt commissionAmt = new MCommissionAmt(A_PO.getCtx(), commissionDetail.getC_CommissionAmt_ID(),commissionDetail.get_TrxName());
		commissionAmt.updateCommissionAmount();
		commissionAmt.saveEx();
		return "";
	}

	private String OrderSetPrecision(PO A_PO) {
		MOrder order = (MOrder)A_PO;
		if (order.getGrandTotal().scale() > order.getC_Currency().getStdPrecision())
			order.setGrandTotal(order.getGrandTotal().setScale(order.getC_Currency().getStdPrecision(), BigDecimal.ROUND_HALF_UP));
		order.saveEx();
		return "";
	}
	


	private String InvoiceSetPrecision(PO A_PO) {
		MInvoice invoice = (MInvoice)A_PO;
		if (invoice.getGrandTotal().scale() > invoice.getC_Currency().getStdPrecision())
			invoice.setGrandTotal(invoice.getGrandTotal().setScale(invoice.getC_Currency().getStdPrecision(), BigDecimal.ROUND_HALF_UP));
		invoice.saveEx();
		return "";
	}
	
	private String PaymentAutoReconcile(PO A_PO) {
		MPayment payment = (MPayment)A_PO;
		//if (payment.getTenderType().equals(MPayment.TENDERTYPE_Cash))
		//	return "";
		MBankAccount bankAccount = (MBankAccount)payment.getC_BankAccount();
		Boolean isAutoReconciled = bankAccount.get_ValueAsBoolean("isAutoReconciled");
		if(isAutoReconciled) {
			int[] bsls = MBankStatementLine.getAllIDs(MBankStatementLine.Table_Name, "c_Payment_ID=" + payment.getC_Payment_ID(), payment.get_TrxName());
			MBankStatementLine bsl = null;
			if (bsls.length==0) {
				bsl = MBankStatement.addPayment(payment);
			}
			else
				bsl = new MBankStatementLine(payment.getCtx(), bsls[0], payment.get_TrxName());
			//if (bsl.getParent().getC_BankAccount().getC_Bank().getBankType().equals((MBank.BANKTYPE_CashJournal)))
			//		bsl.getParent().processIt("CO");
			//		bsl.getParent().saveEx();
			}
		return "";
	}
	

	private String TimeExpenseReportCreateProjectIssue(PO A_PO) {
		MTimeExpense timeExpense = (MTimeExpense)A_PO;
		for (MTimeExpenseLine expenseLine:timeExpense.getLines()) {
			createLine(expenseLine, expenseLine.getQty());
		}
		return "";
	}
	
	private void createLine(MTimeExpenseLine timeExpenseLine, BigDecimal movementQty) {
		//	Create Issue
		if (timeExpenseLine.getC_Project_ID() == 0)
			return;
		MProject project = (MProject)timeExpenseLine.getC_Project();
		MTimeExpense timeExpense = (MTimeExpense)timeExpenseLine.getS_TimeExpense();
		MProjectIssue projectIssue = new MProjectIssue(project);
		projectIssue.setMandatory(timeExpense.getM_Locator_ID(), timeExpenseLine.getM_Product_ID(), timeExpenseLine.getQty());
		
		projectIssue.setMovementDate(timeExpenseLine.getDateExpense());
		projectIssue.setDescription(timeExpenseLine.getDescription());
		projectIssue.setS_TimeExpenseLine_ID(timeExpenseLine.getS_TimeExpenseLine_ID());
		if (timeExpenseLine.getC_ProjectPhase_ID() != 0)
			projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectPhase_ID, timeExpenseLine.getC_ProjectPhase_ID());
		if (timeExpenseLine.getC_ProjectTask_ID()!=0)
			projectIssue.set_ValueOfColumn(MInOutLine.COLUMNNAME_C_ProjectTask_ID, timeExpenseLine.getC_ProjectTask_ID());
		projectIssue.saveEx();
		projectIssue.completeIt();

		//	Find/Create Project Line
		//	Find/Create Project Line
		MProjectLine projectLine = new MProjectLine(project);
		projectLine.setMProjectIssue(projectIssue);		//	setIssueBigDecimal feeAmt = (BigDecimal)expenseLine.get_Value("FeeAmt");
		BigDecimal travelCost =  (BigDecimal)timeExpenseLine.get_Value("TravelCost");
		BigDecimal prepaymentAmt =  (BigDecimal)timeExpenseLine.get_Value("PrepaymentAmt");
		BigDecimal feeAmt = (BigDecimal)timeExpenseLine.get_Value("FeeAmt");
		//projectLine.setCommittedAmt(timeExpenseLine.getConvertedAmt().add(timeExpenseLine.getPriceReimbursed()));
		projectLine.setCommittedAmt(timeExpenseLine.getConvertedAmt().add(feeAmt).add(prepaymentAmt).add(travelCost));
		projectLine.setM_Product_ID(projectIssue.getM_Product_ID());
		projectLine.set_ValueOfColumn(MTimeExpenseLine.COLUMNNAME_S_TimeExpenseLine_ID, projectIssue.getS_TimeExpenseLine_ID());
		projectLine.saveEx();
		//return "@Created@ " + counter.get();		
	}
	
	private void updateLine(MTimeExpenseLine timeExpenseLine) {
		//	Create Issue

		if (!timeExpenseLine.getParent().getDocStatus().equals("CO"))		
			return ;
		String sqlUpdate = 
				"update c_ProjectLine pl  set committedamt = (select coalesce(convertedamt,0) + coalesce(pricereimbursed,0) from s_timeexpenseline where s_Timeexpenseline_ID=pl.s_Timeexpenseline_ID)" + 
				" where pl.s_timeexpenseline_ID = " + timeExpenseLine.getS_TimeExpenseLine_ID();
		int no = DB.executeUpdateEx(sqlUpdate,  timeExpenseLine.get_TrxName());
		
	}
	

	private String factAcct_UpdateDocumentNO(PO A_PO)

	{	
		Doc doc = A_PO.getDoc();
		String dateacct = "DateAcct";
		String Documentno = "";
		if (A_PO instanceof MInventory
				|| A_PO instanceof MMovement
				|| A_PO instanceof MProjectIssue
				|| A_PO instanceof MProduction)
			dateacct = "MovementDate";
		else if (A_PO instanceof MBankStatement)
			dateacct = "StatementDate";
		else if (A_PO instanceof MDDOrder)
			dateacct = "DateOrdered";
		else if (A_PO instanceof MRequisition)
			dateacct = "DateRequired";
		else if (A_PO instanceof MPPOrder)
			dateacct = "DateOrdered";
		Timestamp date = (Timestamp)A_PO.get_Value(dateacct);
		String whereClause = "dateacct =? and documentno is not null and documentno <> ''  and postingtype = 'A' ";
		MFactAcct factacct = new Query(A_PO.getCtx(), MFactAcct.Table_Name, whereClause, A_PO.get_TrxName())
				.setParameters(A_PO.get_Value(dateacct))
				.setClient_ID()
				.first();
		if (factacct != null)
			Documentno = factacct.get_ValueAsString("DocumentNo");
		else
		{
			Documentno = DB.getDocumentNo(A_PO.getAD_Client_ID(), MFactAcct.Table_Name, A_PO.get_TrxName());			
		}


		ArrayList<Fact> facts = doc.getFacts();
		// one fact per acctschema
		for (Fact fact:facts)
		{
			for (FactLine fLine:fact.getLines())
			{
				fLine.set_ValueOfColumn("DocumentNo", Documentno);
			}
		}		
		return "";
	}
	
	private String factAcct_DeleteNullLines(PO A_PO)

	{	
		Doc doc = A_PO.getDoc();
		String dateacct = "DateAcct";
		String Documentno = "";
		
		String whereClause = "AD_Table_ID=? AND record_ID=?";
		
		ArrayList<Fact> facts = doc.getFacts();
		// one fact per acctschema
		for (Fact fact:facts)
		{
			for (FactLine fLine:fact.getLines())
			{
				if (fLine.getAmtSourceDr().compareTo(Env.ZERO)==0 &&  fLine.getAmtSourceCr().compareTo(Env.ZERO)==0)
					fLine.deleteEx(true, A_PO.get_TrxName());
			}
		}		
		return "";
	}
	
	private String setOrderLineTax(PO A_PO) {

		//import org.compoere.model.MProduct;
		//import org.compoere.model.MBPartner;
		//import org.compoere.model.MCharge;
		//import org.compoere.model.X_C_TaxDefinition;
		//import org.compoere.model.Query;

		//import java.util.ArrayList;
		MOrderLine orderLine = (MOrderLine)A_PO;
		Boolean	isSOTrx = orderLine.getParent().isSOTrx();
		int shipC_BPartner = orderLine.getParent().getC_BPartner_ID();
		if (orderLine.getM_Product_ID() == 0 && orderLine.getC_Charge_ID() == 0)
			return "";		//
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(isSOTrx);
		//	Check Partner Location
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("c_Tax_ID in (select c_Tax_ID from c_Tax t where case when ? = 'Y' then t.sopotype in ('B','S') else t.sopotype in('B','P') end)");
		whereClause.append(" and (c_taxgroup_ID =? or c_taxgroup_ID is null)");
		MBPartner bpartner = new MBPartner(Env.getCtx(), shipC_BPartner, null);
		params.add(bpartner.getC_TaxGroup_ID());
		if (orderLine.getC_Charge_ID() != 0)
		{
			whereClause.append(" AND (c_taxtype_ID =? or c_taxtype_ID is null)");
			params.add(orderLine.getC_Charge().getC_TaxType_ID());
		}
		else if (orderLine.getM_Product_ID() != 0)
		{
			whereClause.append(" AND (C_Taxtype_ID =? or C_TaxType_ID is null)");
			params.add(orderLine.getM_Product().getC_TaxType_ID());
		}
		X_C_TaxDefinition taxdefinition = new Query(Env.getCtx(), X_C_TaxDefinition.Table_Name, whereClause.toString(), null)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setParameters(params)
				.setOrderBy("seqNo")
				.first();
		if (taxdefinition == null || taxdefinition.getC_Tax_ID() == 0) {
			return "";		}
		else {
			orderLine.setC_Tax_ID(taxdefinition.getC_Tax_ID());
		}
		Integer i = orderLine.get_ID();
		Object object  = (Object)i;
		//
		return "";
	}
	

	
	private String setInvoiceLineTax(PO A_PO) {

		//import org.compoere.model.MProduct;
		//import org.compoere.model.MBPartner;
		//import org.compoere.model.MCharge;
		//import org.compoere.model.X_C_TaxDefinition;
		//import org.compoere.model.Query;

		//import java.util.ArrayList;
		MInvoiceLine invoiceLine = (MInvoiceLine)A_PO;
		if (invoiceLine.getC_OrderLine_ID() >0)
			return "";
		Boolean	isSOTrx = invoiceLine.getParent().isSOTrx();
		int shipC_BPartner = invoiceLine.getParent().getC_BPartner_ID();
		if (invoiceLine.getM_Product_ID() == 0 && invoiceLine.getC_Charge_ID() == 0)
			return "";		//
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(isSOTrx);
		//	Check Partner Location
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("c_Tax_ID in (select c_Tax_ID from c_Tax t where case when ? = 'Y' then t.sopotype in ('B','S') else t.sopotype in('B','P') end)");
		whereClause.append(" and (c_taxgroup_ID =? or c_taxgroup_ID is null)");
		MBPartner bpartner = new MBPartner(Env.getCtx(), shipC_BPartner, null);
		params.add(bpartner.getC_TaxGroup_ID());
		if (invoiceLine.getC_Charge_ID() != 0)
		{
			whereClause.append(" AND (c_taxtype_ID =? or c_taxtype_ID is null)");
			params.add(invoiceLine.getC_Charge().getC_TaxType_ID());
		}
		else if (invoiceLine.getM_Product_ID() != 0)
		{
			whereClause.append(" AND (C_Taxtype_ID =? or C_TaxType_ID is null)");
			params.add(invoiceLine.getM_Product().getC_TaxType_ID());
		}
		X_C_TaxDefinition taxdefinition = new Query(Env.getCtx(), X_C_TaxDefinition.Table_Name, whereClause.toString(), null)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setParameters(params)
				.setOrderBy("seqNo")
				.first();
		if (taxdefinition == null || taxdefinition.getC_Tax_ID() == 0) {
			return "";		}
		else
			invoiceLine.setC_Tax_ID(taxdefinition.getC_Tax_ID());
		//
		return "";
	}
	
	private String UpdatePaymentWithholing(PO A_PO) {
		return "";
	}
	

	private String UpdateCreditMemo(PO A_PO) {
		MInvoice invoice = (MInvoice)A_PO;
		if (!invoice.getC_DocTypeTarget().getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)
				|| invoice.getM_RMA_ID() ==0)
			return "";
		invoice.setPOReference(getInvoiceDocNo(invoice));
		return "";
	}
	
	private String UpdateCreditMemoLine(PO A_PO) {
		MInvoiceLine invoiceLine = (MInvoiceLine)A_PO;
		if (!invoiceLine.getC_Invoice().getC_DocTypeTarget().getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)
				|| invoiceLine.getC_Invoice().getM_RMA_ID() ==0)
			return "";
		if (invoiceLine.getM_Product_ID() > 0)
			invoiceLine.setDescription(invoiceLine.getM_Product().getValue() + "; " + invoiceLine.getM_Product().getName() + "; " 
					+ invoiceLine.getPriceActual().toString());
		return "";
	}
	
	private String getInvoiceDocNo(MInvoice invoice) {
		String docNoList = "";
		String dateList = "";
		
			String sql = "SELECT distinct iorg.documentno, iorg.dateInvoiced, iorg.docstatus FROM C_Invoice i " + 
					" INNER JOIN C_InvoiceLine ivl on i.c_INvoice_ID=ivl.c_Invoice_ID " + 
					" INNER JOIN C_OrderLine ol on ivl.C_OrderLine_ID=ol.c_OrderLine_ID " + 
					" LEFT JOIN c_InvoiceLine il on il.c_OrderLIne_ID=ol.c_OrderLine_ID and il.c_INvoiceline_ID <> ivl.c_INvoiceline_ID " + 
					" LEFT JOIN C_Invoice iorg on il.c_Invoice_ID=iorg.c_Invoice_ID\r\n" + 
					" WHERE i.C_Invoice_ID=? and iorg.docstatus in ('CO','CL', 'VO') order by iorg.docstatus ";
			PreparedStatement pstmt = null;
			try
			{
				pstmt = DB.prepareStatement(sql, invoice.get_TrxName());
				pstmt.setInt(1, invoice.getC_Invoice_ID());
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					docNoList = docNoList + rs.getString(1);
					Timestamp date = rs.getTimestamp(2);
					String stringDate = new SimpleDateFormat("dd/MM/yyyy").format(date); 
					dateList = dateList + " " + stringDate;
				}
				rs.close();
				pstmt.close();
				pstmt = null;
				if (docNoList == null)
					docNoList = "";
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "getName", e);
			}
			finally
			{
				try
				{
					if (pstmt != null)
						pstmt.close ();
				}
				catch (Exception e)
				{}
				pstmt = null;
			}
		return docNoList + ";" + dateList;
	}
	
	private String controlInvoiceTax(PO po) {
		MInvoice invoice = (MInvoice)po;
		BigDecimal totallines = invoice.getTotalLines().setScale(2);
		String sql = "select sum(round(linenetamt, 2)) from c_INvoiceline ivl where ivl.c_INvoice_ID=?";
		BigDecimal totallines_Rounded = 
				DB.getSQLValueBDEx(po.get_TrxName(), sql, invoice.getC_Invoice_ID());
		BigDecimal diff = totallines.subtract(totallines_Rounded);
		if (diff.compareTo(Env.ZERO) == 0)
			return "";
		
		return "";		
	}

	private String testQtyOnhand(PO po) {
		MOrder order = (MOrder)po;
		if (!order.isSOTrx())
			return "";
		String error = "";
		Boolean nottest = (order.getC_DocType().getDocSubTypeSO().equals(MOrder.DocSubTypeSO_RMA) ||
				order.getC_DocType().getDocSubTypeSO().equals(MOrder.DocSubTypeSO_Standard) );
		if (nottest)
			return "";
		StringBuffer sql = new StringBuffer("SELECT COALESCE(SUM(s.QtyOnHand),0)")
				.append(" FROM M_Storage s")
				.append(" WHERE s.M_Product_ID=?");
		sql.append(" AND EXISTS (SELECT 1 FROM M_Locator l WHERE s.M_Locator_ID=l.M_Locator_ID AND l.M_Warehouse_ID=?)");
		ArrayList<Object> params = new ArrayList<Object>();

		for (MOrderLine orderLine:order.getLines())
		{
			if (orderLine.isDescription() || orderLine.getC_Charge_ID() != 0 || !orderLine.getM_Product().isStocked()
					|| orderLine.getQtyOrdered().signum()<0)
				continue;
			StringBuffer sqlFinal = new StringBuffer(sql);
			params.clear();
			params.add(orderLine.getM_Product_ID());
			// Warehouse level
			params.add(order.getM_Warehouse_ID());
			// With ASI
			if (orderLine.getM_AttributeSetInstance_ID() != 0) {
				sqlFinal.append(" AND s.M_AttributeSetInstance_ID=?");
				params.add(orderLine.getM_AttributeSetInstance_ID());
			}
			//
			BigDecimal qtyOnHand = DB.getSQLValueBD(order.get_TrxName(), sqlFinal.toString(), params);
			if (qtyOnHand.subtract( orderLine.getQtyOrdered()).signum()< 0)
				error = error + Msg.translate(order.getCtx(), "InsufficientQtyAvailable") + " " + orderLine.getM_Product().getName() + "; ";
		}

		return error;

	}

	private String updateTranslation(PO po) {
		String additionalSQL = " ";
		String tableName = po.get_TableName();
		Boolean ischange = tableName.equals("C_DocType")? po.is_ValueChanged("Name"):
			(po.is_ValueChanged("Name") || po.is_ValueChanged("Description"));
		if (!ischange)
			return "";
		String translationTableName = po.get_TableName().concat("_Trl");
		String columnName = po.get_TableName().concat("_ID");
		int ID = po.get_ValueAsInt(columnName);

		if (!tableName.equals("C_DocType")){
			additionalSQL = "', description ='" + po.get_ValueAsString("Description");
		}

		List<Object> params = new ArrayList<Object>();
		params.add(ID);
		params.add(Env.getAD_Language(po.getCtx()));
		String sql = "Update " + translationTableName + " set name ='" + po.get_Value("Name") + additionalSQL +
				"' where " + columnName +" =? AND AD_Language = ?";
		DB.executeUpdateEx(sql,params.toArray(new Object[params.size()]), po.get_TrxName());
		return ""; 
	}

	private String updateUser4Order(PO po) {
		MOrder order = (MOrder)po;
		if (order.isSOTrx())
			return "";
		if (order.getUser4_ID() > 0) {
			MElementValue user4 = (MElementValue)order.getUser4();
			user4.set_CustomColumn("ValidFrom", order.getDateOrdered());
			user4.saveEx();
		}
		return "";
	}

	private String updateUser4MaterialReceipt(PO po) {
		MInOut inOut = (MInOut)po;
		if (inOut.isSOTrx())
			return "";
		if (inOut.getUser4_ID() > 0) {
			MElementValue user4 = (MElementValue)inOut.getUser4();
			user4.set_CustomColumn("ValidTo", inOut.getMovementDate());
			user4.saveEx();
		}
		return "";
	}
	
	private String allocationHdrBeforeDelete(PO po) {
		MAllocationHdr allocationHdr = (MAllocationHdr)po;
		
		Arrays.stream(allocationHdr.getLines(false))
		.forEach( allocationLine -> {
			if (allocationLine.getC_Invoice_ID() != 0) {
				MInvoice invoice = (MInvoice)allocationLine.getC_Invoice();
				invoice.setIsPaid(false);
				invoice.saveEx();
			}
			if (allocationLine.getC_Payment_ID() !=0) {
				MPayment payment = (MPayment)allocationLine.getC_Payment();
				payment.setIsAllocated(false);
				payment.saveEx();
			}
		});
		return "";
	}
	
	private String productPOCheckCurrentVendor(PO po) {
		MProductPO productPO = (MProductPO)po;
		if (!productPO.isCurrentVendor())
			return "";
		String sql = "Select count(*) from m_Product_PO where m_Product_ID=? and iscurrentvendor = 'Y'";
		List<Object> params = new ArrayList<>();
		params.add(productPO.getM_Product_ID());
		int no = DB.getSQLValueEx(productPO.get_TrxName(), sql, params);
 
		return no==0? "": "Ya existe un proveedor predeterminado";
	}
	
	private String paySelectionLineUpdatebpartnerName(PO po) {
		MPaySelectionLine line = (MPaySelectionLine)po;
		if (line.get_ValueAsString("bpartnername") == "")
			line.set_CustomColumn("bpartnername", line.getC_BPartner().getName());
		return "";		
	}
	
	 public Timestamp nextBusinessDay (Timestamp day, String trxName) throws SQLException
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//
		//begin Goodwill (www.goodwill.co.id)
		// get Holiday
		boolean isHoliday = true;
		do
		{
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			if (dow == Calendar.SATURDAY)
				cal.add(Calendar.DAY_OF_YEAR, 2);
			else if (dow == Calendar.SUNDAY)
				cal.add(Calendar.DAY_OF_YEAR, 1);
			java.util.Date temp = cal.getTime();
			String sql = "SELECT Date1 FROM C_NonBusinessDay WHERE IsActive ='Y' AND Date1=?";
			//PreparedStatement pstmt = Adempiere.prepareStatement(sql);
			PreparedStatement pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setTimestamp(1,new Timestamp(temp.getTime()));
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				cal = new GregorianCalendar();
				cal.setTime(temp);
				cal.add(Calendar.DAY_OF_YEAR,1);
			}
			else 
				isHoliday = false;
			
			rs.close();
			pstmt.close();

		}
		while (isHoliday);
		// end Goodwill
		
		return new Timestamp (cal.getTimeInMillis());
	}	//	nextBusinessDay	
	 
	 private String controlPriceListDocType(PO po) {
		 String error = "";
		 MOrder order = (MOrder)po;
		 if (!order.isSOTrx())
			 return "";
		 MDocType docType = (MDocType)order.getC_DocTypeTarget();
		 if (docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_WarehouseOrder)
				 || docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_ReturnMaterial))
			 return "";
		 Boolean isTaxIncluded = docType.get_ValueAsBoolean(MPriceList.COLUMNNAME_IsTaxIncluded);
		 if (isTaxIncluded != order.getM_PriceList().isTaxIncluded())
			 error = "Lista de Precio y Tipo de documento no coinciden ";
		 return error;
	 }
	 
	 private String OrderBeforePrepare(PO po) {
		 MOrder order = (MOrder)po;
		 int stdPrecision = MCurrency.getStdPrecision(po.getCtx(), order.getC_Currency_ID());
		 if (order.getTotalLines().scale() > stdPrecision) {
			 BigDecimal totalLinesRounded = Env.ZERO;
			 for (MOrderLine orderLine: order.getLines()) {
				 BigDecimal linenetamt = orderLine.getLineNetAmt().setScale(stdPrecision, BigDecimal.ROUND_HALF_UP);
				 orderLine.saveEx();
				 totalLinesRounded = totalLinesRounded.add(linenetamt);
			 }
			 order.setTotalLines(totalLinesRounded);
		 }
		 return "";
	 }
	 
	 private Boolean checkQtyOnHand(MMovementLine movementLine) {
			// Begin Armen 2006/10/01
			if (movementLine.getM_Product_ID() != 0) {
				if (movementLine.getM_Product().isStocked()) {					
					int M_Locator_ID = movementLine.getM_Locator_ID();
					BigDecimal qtyOnHand = getQtyOnHand(M_Locator_ID, movementLine.getM_Product_ID(), 
							movementLine.getM_AttributeSetInstance_ID(), movementLine.get_TrxName());
					if (qtyOnHand == null)
						qtyOnHand = Env.ZERO;
					if (qtyOnHand.compareTo(movementLine.getMovementQty()) < 0)
						return false;
				}
			}
			return true;
	 }
	 

		
		/**
		 * Get Warehouse/Locator Available Qty.
		 * The call is accurate only if there is a storage record 
		 * and assumes that the product is stocked 
		 * @param M_Warehouse_ID wh (if the M_Locator_ID!=0 then M_Warehouse_ID is ignored)
		 * @param M_Locator_ID locator (if 0, the whole warehouse will be evaluated)
		 * @param M_Product_ID product
		 * @param M_AttributeSetInstance_ID masi
		 * @param trxName transaction
		 * @return qty available (QtyOnHand-QtyReserved) or null if error
		 */
		public  BigDecimal getQtyOnHand (int M_Locator_ID, 
			int M_Product_ID, int M_AttributeSetInstance_ID, String trxName)
		{
			ArrayList<Object> params = new ArrayList<Object>();
			StringBuffer sql = new StringBuffer("SELECT COALESCE(SUM(s.QtyOnHand),0)")
									.append(" FROM M_Storage s")
									.append(" WHERE s.M_Product_ID=?");
			params.add(M_Product_ID);
			// Warehouse level
			// Locator level
				sql.append(" AND s.M_Locator_ID=?");
				params.add(M_Locator_ID);
			// With ASI
			if (M_AttributeSetInstance_ID != 0) {
				sql.append(" AND s.M_AttributeSetInstance_ID=?");
				params.add(M_AttributeSetInstance_ID);
			}
			//
			BigDecimal retValue = DB.getSQLValueBD(trxName, sql.toString(), params);
			return retValue;
		}	//	getQtyOnHand
		
		private String movementBeforePrepare(PO po) {
			Hashtable<Integer, BigDecimal> storages = new Hashtable<Integer, BigDecimal>();
			MMovement movement = (MMovement)po;
			String errorMsg = "";
			BigDecimal qtyAvailable = Env.ZERO;
			for (MMovementLine movementLine: movement.getLines(true)) {
				int key = movementLine.getM_Product_ID() + movementLine.getM_AttributeSetInstance_ID() + movementLine.getM_Locator_ID();
				qtyAvailable = storages.get(key);			
				if(qtyAvailable == null) {
				qtyAvailable = getQtyOnHand( movementLine.getM_Locator_ID(),
							movementLine.getM_Product_ID(),movementLine.getM_AttributeSetInstance_ID(), po.get_TrxName());
					storages.put(key, qtyAvailable);
				}
				qtyAvailable = qtyAvailable.subtract(movementLine.getMovementQty());
				storages.replace(key, qtyAvailable);
				if (qtyAvailable.compareTo(Env.ZERO)<0) {					
					errorMsg = Msg.translate(po.getCtx(), "InsufficientQtyAvailable") + " " + movementLine.getM_Product().getName();
				break;
				}
			}			
			return errorMsg;
		}
		private String inoutLineSetOrderLine(PO po) {
			MInOut inOut = (MInOut)po;
			if (inOut.isSOTrx() || inOut.getC_Order_ID() <=0) {
				return "";
			}
			Arrays.stream(inOut.getLines(false)).filter(inoutLine -> 
					 inoutLine.getC_OrderLine_ID() <=0)
			.forEach( inOutLine -> {
				MOrder order = (MOrder)inOutLine.getParent().getC_Order();
				for (MOrderLine orderLine : order.getLines()) {
					if (orderLine.getM_Product_ID() == inOutLine.getM_Product_ID() && orderLine.getM_AttributeSetInstance_ID() <=0) {
						inOutLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
						inOutLine.saveEx();
						break;
					}
				}
			});
			
			return "";
			
		}
		
		private String updateDDOrderLine(PO po) {
			MMovement move = (MMovement)po;
			for (MMovementLine line : move.getLines(false))
			{
				if(line.getDD_OrderLine_ID() > 0)
				{
					MDDOrderLine oline= new MDDOrderLine(line.getCtx(),line.getDD_OrderLine_ID(), po.get_TrxName());
					MLocator locator_to = MLocator.get(line.getCtx(), line.getM_LocatorTo_ID());
					MWarehouse warehouse =  MWarehouse.get(line.getCtx(), locator_to.getM_Warehouse_ID()); 
					oline.setQtyInTransit(oline.getQtyInTransit().subtract(line.getMovementQty()));
					oline.setQtyDelivered(oline.getQtyDelivered().add(line.getMovementQty()));
					oline.saveEx();

				}
			}
			return "";
		}
		
		private String updateWMInOutBoundLine(PO po) {

			MDDOrderLine orderLine = (MDDOrderLine) po;
			MWMInOutBoundLine outboundLine = (MWMInOutBoundLine) orderLine.getWM_InOutBoundLine();
			if (outboundLine != null
			&& outboundLine.getWM_InOutBoundLine_ID() > 0
			&& orderLine.getQtyOrdered().compareTo(orderLine.getQtyDelivered()) >= 0) {
				BigDecimal pickedQuantity = outboundLine.getPickedQty();
				BigDecimal totalPickedQuantity = pickedQuantity.add(orderLine.getQtyDelivered());
				outboundLine.setPickedQty(totalPickedQuantity);
				outboundLine.saveEx();
			}
			
			return "";
		}

	
		
			
		

	
}	//	Validator
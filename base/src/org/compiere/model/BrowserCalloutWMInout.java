package org.compiere.model;

/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpcya.com                                 *
 *****************************************************************************/
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.grid.BrowserCallOutEngine;
import org.eevolution.grid.BrowserRow;

/**
 * 	Project Browser Callout
 * 	@author Mario Calderon, mario.calderon@westfalia-it.com, Systemhaus Westfalia http://www.westfalia-it.com
 * 
 */
public class BrowserCalloutWMInout extends BrowserCallOutEngine {

	/**
	 *	Product in Task updates
	 *		- List Price
	 *		- Price Entered
	 *		- Line Amount
	 *		- Actual Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptProduct (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}
		
		//	No changes ?
		if( (Integer)oldValue==M_Product_ID)
			return "";		

		Integer C_Project_ID = (Integer) row.getValue("P_C_Project_ID");
		MProject project = MProject.getById(Env.getCtx(), C_Project_ID, null);
		
		// PriceList or PriceListVersion mandatory
 		if(project.getM_PriceList_ID()==0 && project.getM_PriceList_Version_ID()==0) {
			String info = Msg.parseTranslation(ctx, "@PriceListNotFound@" + ", " + "@PriceListVersionNotFound@");
			// TODO Fire error  ("Error", info, false
			//mTab.fireDataStatusEEvent ("Error", info, false);
			return "";		
		}
 		
 		BigDecimal listPrice = getPriceList(project, M_Product_ID, ctx); 		
 		BigDecimal qty = (BigDecimal) row.getValue("PT_Qty");
 		row.setValue("PT_PriceList", listPrice);
 		row.setValue("PT_PriceEntered", listPrice);
 		row.setValue("PT_ActualAmt", qty.multiply(listPrice));
 		row.setValue("PT_LineNetAmt", qty.multiply(listPrice));
 		row.setValue("PT_MarginAmt", Env.ZERO);
 		row.setValue("PT_Margin", Env.ZERO);
		
		return "";
	}	//	ptProduct
	
	/**
	 *	Quantity in Task updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptQty (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PT_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal qty = (BigDecimal)value;
		BigDecimal listPrice = (BigDecimal) row.getValue("PT_PriceList");
		BigDecimal priceEntered = (BigDecimal) row.getValue("PT_PriceEntered");
		
		
		// In ZK it can happen that callout "ptProduct" is not called
		// Here a workaround is implemented
		if(listPrice==null)  {
			Integer C_Project_ID = (Integer) row.getValue("P_C_Project_ID");
			MProject project = MProject.getById(Env.getCtx(), C_Project_ID, null);
			listPrice = getPriceList(project, M_Product_ID, ctx);
			row.setValue("PT_PriceList", listPrice);
			priceEntered = listPrice;
			row.setValue("PT_PriceEntered", priceEntered);
		}
		
		BigDecimal lineAmt = qty.multiply(listPrice);
		BigDecimal actualAmt = qty.multiply(priceEntered);
		row.setValue("PT_LineNetAmt", lineAmt);
		row.setValue("PT_ActualAmt", actualAmt);
		row.setValue("PT_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		if (lineAmt.compareTo(Env.ZERO)!=0)
			row.setValue("PT_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
					.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			row.setValue("PT_Margin", Env.ZERO);

		return "";
	}	//	ptQty
	
	/**
	 *	Price Entered in Task updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptPriceEntered (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PT_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal priceEntered =  (BigDecimal)value;
		if (priceEntered.compareTo(Env.ZERO)==0) {
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", ((BigDecimal) row.getValue("PT_LineNetAmt")).negate());
			row.setValue("PT_Margin", Env.ONEHUNDRED.negate());
		}			
		else  {
			BigDecimal qty = (BigDecimal) row.getValue("PT_Qty");
			BigDecimal listPrice = (BigDecimal) row.getValue("PT_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			BigDecimal actualAmt = qty.multiply(priceEntered);
			row.setValue("PT_LineNetAmt", lineAmt);
			row.setValue("PT_ActualAmt", actualAmt);
			row.setValue("PT_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PT_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		}	
		return "";
	}	//	ptPriceEntered
	
	/**
	 *	Actual Amount in Task updates
	 *		- Price Entered
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptActualAmt (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PT_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal actualAmt = (BigDecimal)value;
		if (actualAmt.compareTo(Env.ZERO)==0) {
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", ((BigDecimal) row.getValue("PT_LineNetAmt")).negate());
			row.setValue("PT_Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PT_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
		}
		else {
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PT_PriceEntered", priceEntered);
			BigDecimal listPrice = (BigDecimal) row.getValue("PT_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PT_LineNetAmt", lineAmt);
			
			row.setValue("PT_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PT_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	ptActualAmt
	
	/**
	 *	Margin (%) in Task updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin Amount
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptMarginPercentage (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PT_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal margin = (BigDecimal)value;
		if (margin.compareTo(Env.ONEHUNDRED.negate())==-1) {
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", ((BigDecimal) row.getValue("PT_LineNetAmt")).negate());
			row.setValue("PT_Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PT_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) row.getValue("PT_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PT_LineNetAmt", lineAmt);
			BigDecimal actualAmt = margin.divide(Env.ONEHUNDRED, 6, BigDecimal.ROUND_HALF_UP).add(Env.ONE).multiply(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
			row.setValue("PT_ActualAmt", actualAmt);
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PT_PriceEntered", priceEntered);
			row.setValue("PT_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		}
		return "";
	}	//	ptMarginPercentage
	
	/**
	 *	Margin Amount in Task updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin %
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ptMarginAmt (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PT_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PT_PriceList", Env.ZERO);
			row.setValue("PT_PriceEntered", Env.ZERO);
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}
		
		BigDecimal marginAmt = (BigDecimal)value;
		if (marginAmt.compareTo(Env.ZERO)==0) {
			row.setValue("PT_PriceEntered", (BigDecimal) row.getValue("PT_PriceList"));
			row.setValue("PT_ActualAmt", (BigDecimal) row.getValue("PT_LineNetAmt"));
			row.setValue("PT_Margin", Env.ZERO);
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PT_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PT_LineNetAmt", Env.ZERO);
			row.setValue("PT_ActualAmt", Env.ZERO);
			row.setValue("PT_MarginAmt", Env.ZERO);
			row.setValue("PT_Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) row.getValue("PT_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PT_LineNetAmt", lineAmt);

			BigDecimal actualAmt = marginAmt.add(lineAmt);
			row.setValue("PT_ActualAmt", actualAmt);

			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PT_PriceEntered", priceEntered);
			
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PT_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	ptMarginAmt
	
	
	/**
	 *	Product in Phase updates
	 *		- List Price
	 *		- Price Entered
	 *		- Line Amount
	 *		- Actual Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppProduct (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}
		
		//	No changes ?
		if( (Integer)oldValue==M_Product_ID)
			return "";		

		Integer C_Project_ID = (Integer) row.getValue("P_C_Project_ID");
		MProject project = MProject.getById(Env.getCtx(), C_Project_ID, null);
		
		// PriceList or PriceListVersion mandatory
 		if(project.getM_PriceList_ID()==0 && project.getM_PriceList_Version_ID()==0) {
			String info = Msg.parseTranslation(ctx, "@PriceListNotFound@" + ", " + "@PriceListVersionNotFound@");
			// TODO Fire error  ("Error", info, false
			//mTab.fireDataStatusEEvent ("Error", info, false);
		}		

 		BigDecimal listPrice = getPriceList(project, M_Product_ID, ctx); 
 		BigDecimal qty = (BigDecimal) row.getValue("PP_Qty");
 		row.setValue("PP_PriceList", listPrice);
 		row.setValue("PP_PriceEntered", listPrice);
 		row.setValue("PP_ActualAmt", qty.multiply(listPrice));
 		row.setValue("PP_LineNetAmt", qty.multiply(listPrice));
 		row.setValue("PP_MarginAmt", Env.ZERO);
 		row.setValue("PP_Margin", Env.ZERO);
		
		return "";
	}	//	ppProduct
	
	
	/**
	 *	Quantity in Phase updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppQty (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PP_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}
		
		BigDecimal qty = (BigDecimal)value;
		BigDecimal listPrice = (BigDecimal) row.getValue("PP_PriceList");
		BigDecimal priceEntered = (BigDecimal) row.getValue("PP_PriceEntered");		
		
		// In ZK it can happen that callout "ptProduct" is not called
		// Here a workaround is implemented
		if(listPrice==null)  {
			Integer C_Project_ID = (Integer) row.getValue("P_C_Project_ID");
			MProject project = MProject.getById(Env.getCtx(), C_Project_ID, null);
			listPrice = getPriceList(project, M_Product_ID, ctx);
			row.setValue("PP_PriceList", listPrice);
			priceEntered = listPrice;
			row.setValue("PP_PriceEntered", priceEntered);
		}
		
		BigDecimal lineAmt = qty.multiply(listPrice);
		BigDecimal actualAmt = qty.multiply(priceEntered);
		row.setValue("PP_LineNetAmt", lineAmt);
		row.setValue("PP_ActualAmt", actualAmt);
		row.setValue("PP_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		if (lineAmt.compareTo(Env.ZERO)!=0)
			row.setValue("PP_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
					.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			row.setValue("PP_Margin", Env.ZERO);

		return "";
	}	//	ppQty
	
	/**
	 *	Price Entered in Phase updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppPriceEntered (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PP_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal priceEntered =  (BigDecimal)value;
		if (priceEntered.compareTo(Env.ZERO)==0) {
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", ((BigDecimal) row.getValue("PP_LineNetAmt")).negate());
			row.setValue("PP_Margin", Env.ONEHUNDRED.negate());
		}			
		else  {
			BigDecimal qty = (BigDecimal) row.getValue("PP_Qty");
			BigDecimal listPrice = (BigDecimal) row.getValue("PP_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			BigDecimal actualAmt = qty.multiply(priceEntered);
			row.setValue("PP_LineNetAmt", lineAmt);
			row.setValue("PP_ActualAmt", actualAmt);
			row.setValue("PP_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PP_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		}	
		return "";
	}	//	ppPriceEntered
	
	/**
	 *	Actual Amount in Phase updates
	 *		- Price Entered
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppActualAmt (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PP_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal actualAmt = (BigDecimal)value;
		if (actualAmt.compareTo(Env.ZERO)==0) {
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", ((BigDecimal) row.getValue("PP_LineNetAmt")).negate());
			row.setValue("PP_Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PP_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
		}
		else {
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PP_PriceEntered", priceEntered);
			BigDecimal listPrice = (BigDecimal) row.getValue("PP_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PP_LineNetAmt", lineAmt);
			
			row.setValue("PP_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PP_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	ppActualAmt
	
	/**
	 *	Margin (%) in Phase updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin Amount
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppMarginPercentage (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PP_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}

		BigDecimal margin = (BigDecimal)value;
		if (margin.compareTo(Env.ONEHUNDRED.negate())==-1) {
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", ((BigDecimal) row.getValue("PP_LineNetAmt")).negate());
			row.setValue("PP_Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PP_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) row.getValue("PP_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PP_LineNetAmt", lineAmt);
			BigDecimal actualAmt = margin.divide(Env.ONEHUNDRED, 6, BigDecimal.ROUND_HALF_UP).add(Env.ONE).multiply(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
			row.setValue("PP_ActualAmt", actualAmt);
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PP_PriceEntered", priceEntered);
			row.setValue("PP_MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		}
		return "";
	}	//	ppMarginPercentage
	
	/**
	 *	Margin Amount in Phase updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin %
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Browser Row
	 *  @param field   Grid Field
	 *  @param value    The new value
	 *  @param oldValue    The old value
	 *  @param currentRow    The current Row
	 *  @param currentColumn    The current Column
	 *  @return Error message or ""
	 */
	public String ppMarginAmt (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)row.getValue("PP_M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			row. setValue("PP_PriceList", Env.ZERO);
			row.setValue("PP_PriceEntered", Env.ZERO);
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}
		
		BigDecimal marginAmt = (BigDecimal)value;
		if (marginAmt.compareTo(Env.ZERO)==0) {
			row.setValue("PP_PriceEntered", (BigDecimal) row.getValue("PP_PriceList"));
			row.setValue("PP_ActualAmt", (BigDecimal) row.getValue("PP_LineNetAmt"));
			row.setValue("PP_Margin", Env.ZERO);
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) row.getValue("PP_Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			row.setValue("PP_LineNetAmt", Env.ZERO);
			row.setValue("PP_ActualAmt", Env.ZERO);
			row.setValue("PP_MarginAmt", Env.ZERO);
			row.setValue("PP_Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) row.getValue("PP_PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			row.setValue("PP_LineNetAmt", lineAmt);

			BigDecimal actualAmt = marginAmt.add(lineAmt);
			row.setValue("PP_ActualAmt", actualAmt);

			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			row.setValue("PP_PriceEntered", priceEntered);
			
			if (lineAmt.compareTo(Env.ZERO)!=0)
				row.setValue("PP_Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	ppMarginAmt
	

	
	/**
	 *	Get List Price of Product depending on Price List/Price List version of Project
	 *  @param project   Project
	 *  @param M_Product_ID Product ID
	 *  @param ctx      Context
	 *  @return List Price
	 */
	public BigDecimal getPriceList(MProject project, int M_Product_ID, Properties ctx)
	{
		BigDecimal listPrice = new BigDecimal(0);
		int M_PriceList_Version_ID =0;
		if (project.getM_PriceList_Version_ID()==0) {
			String sql = "SELECT plv.M_PriceList_Version_ID "
				+ "FROM M_PriceList_Version plv "
				+ "WHERE plv.M_PriceList_ID=? "						//	1
				+ " AND plv.ValidFrom <= ? "
				+ " AND plv.isActive = 'Y' "
				+ "ORDER BY plv.ValidFrom DESC";
			//	Use newest price list - may not be future
			
			Timestamp dateStartSchedule = project.getDateStartSchedule();
			M_PriceList_Version_ID = DB.getSQLValueEx(null, sql, project.getM_PriceList_ID(), dateStartSchedule);
			//priceListVersion = new MPriceListVersion(Env.getCtx(), M_PriceList_Version_ID, null);
			
		}
		else {
			M_PriceList_Version_ID = project.getM_PriceList_Version_ID();
			//priceListVersion = new MPriceListVersion(Env.getCtx(), project.getM_PriceList_Version_ID(), null);
		}
		
		//MProduct product = MProduct.get (Env.getCtx(), M_Product_ID.intValue());
		MProductPrice pp = MProductPrice.get(Env.getCtx(), M_PriceList_Version_ID, M_Product_ID, null);
		if (pp==null) {
			String info = Msg.parseTranslation(ctx, "@ProductNotOnPriceList@");
			// TODO Fire error  ("Error", info, false
			return listPrice;		
		}
		else  {
			listPrice = pp.getPriceList();
		}
		return listPrice;
	}  // getPriceList
	
	public String Selected (Properties ctx,  int WindowNo, BrowserRow row, 
			GridField field, Object value, Object oldValue,int currentRow, int currentColumn)
	{
		if (isCalloutActive())
			return "";
		return "Hihi";
	}
	
	
	
}

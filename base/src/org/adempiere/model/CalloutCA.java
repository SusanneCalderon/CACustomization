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
package org.adempiere.model;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;


import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.Query;
import org.compiere.model.X_C_BPartner;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.eevolution.model.X_C_TaxDefinition;

/**
 *	Order Callouts.
 *	
 *  @author Jorg Janke
 *  @version $Id: CalloutOrder.java,v 1.5 2006/10/08 06:57:33 comdivision Exp $
 *  
 *  @author Michael McKay (mjmckay)
 *  		<li> BF3468458 - Attribute Set Instance not filled on Orders when product lookup not used.
 *  			 See https://sourceforge.net/tracker/?func=detail&aid=3468458&group_id=176962&atid=879332
 */
/**
 * 
 * @author SHW_User
 * SHW Suche nach Steuer ueber Taxdefinition,  Zeile 1302
 */
public class CalloutCA extends CalloutEngine
{
	/**	Debug Steps			*/
	private boolean steps = false;

	/**
	 *	Order Header Change - DocType.
	 *		- InvoiceRule/DeliveryRule/PaymentRule
	 *		- temporary Document
	 *  Context:
	 *  	- DocSubTypeSO
	 *		- HasCharges
	 *	- (re-sets Business Partner info of required)
	 *
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	/**
	 */
	public String bPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BPartner_ID = (Integer)value;
		if (C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";
		 MBPartner bpartner = new MBPartner (ctx, C_BPartner_ID, null);
         int c_doctype_ID = -1;
         boolean IsSOTrx = "Y".equals(Env.getContext(ctx, WindowNo, "IsSOTrx"));
         if (IsSOTrx)
         c_doctype_ID = bpartner.get_ValueAsInt("C_DocType_ID");
         else
             c_doctype_ID = bpartner.get_ValueAsInt("C_DocTypePO_ID");
             if (c_doctype_ID > 0)
            	 mTab.setValue("C_DocTypeTarget_ID", c_doctype_ID);
             int user2_ID = -1;
             user2_ID = bpartner.get_ValueAsInt("User2_ID");
             if (user2_ID > 0)
             mTab.setValue("User2_ID", user2_ID);
             return "";
	}	//	bPartner
	public String product (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{

		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0)
			return "";
		String sql = "Select user1_ID from m_Product where m_Product_ID=?";
		//MProduct product = new MProduct(ctx, M_Product_ID, null);
        int user1_ID = DB.getSQLValueEx(null, sql, M_Product_ID);
        sql = "Select user3_ID from m_Product where m_Product_ID=?";
        int user3_ID =  DB.getSQLValueEx(null, sql, M_Product_ID);
        //user1_ID = product.get_ValueAsInt("User1_ID");
        //user3_ID = product.get_ValueAsInt("User3_ID");
		if (user1_ID>0 )
			mTab.setValue("User1_ID", user1_ID);
		if (user3_ID>0 )
			mTab.setValue("User3_ID", user3_ID);
	return "";
	}	//	product
	
	public String docType (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {

		Integer C_DocType_ID = (Integer)value;
		if (C_DocType_ID == null || C_DocType_ID.intValue() == 0)
			return "";
		String sql = "SELECT d.HasCharges "							//  9..10
				+ "FROM C_DocType d "
				+ "WHERE C_DocType_ID=?";
		String hasCharges = DB.getSQLValueStringEx(null, sql, value);
		Env.setContext(ctx, WindowNo, "HasCharges", hasCharges);
		return "";
	}


	public String taxDefinition (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		//import org.compoere.model.MProduct;
		//import org.compoere.model.MBPartner;
		//import org.compoere.model.MCharge;
		//import org.compoere.model.X_C_TaxDefinition;
		//import org.compoere.model.Query;

		//import java.util.ArrayList;
		String column = mField.getColumnName();
		if (value == null)
			return "";
		if (steps) log.warning("init");
		
		//	Check Product
		int M_Product_ID = 0;
		if (column.equals("M_Product_ID"))
			M_Product_ID = ((Integer)value).intValue();
		else
			M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		int C_Charge_ID = 0;
		if (column.equals("C_Charge_ID"))
			C_Charge_ID = ((Integer)value).intValue();
		else
			C_Charge_ID = Env.getContextAsInt(ctx, WindowNo, "C_Charge_ID");
		log.fine("Product=" + M_Product_ID + ", C_Charge_ID=" + C_Charge_ID);
		if (M_Product_ID == 0 && C_Charge_ID == 0)
			return"";		//

	    ArrayList<Object> params = new ArrayList<Object>();
	    params.add(Env.isSOTrx(ctx, WindowNo));
		//	Check Partner Location
	    StringBuffer whereClause = new StringBuffer();
	    whereClause.append("c_Tax_ID in (select c_Tax_ID from c_Tax t where case when ? = 'Y' then t.sopotype in ('B','S') else t.sopotype in('B','P') end)");
		whereClause.append(" and (c_taxgroup_ID =? or c_taxgroup_ID is null)");
		int	shipC_BPartner = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		MBPartner bpartner = new MBPartner(Env.getCtx(), shipC_BPartner, null);
		params.add(bpartner.getC_TaxGroup_ID());
		if (C_Charge_ID != 0)
		{
			MCharge charge = new MCharge(ctx, C_Charge_ID, null);
			whereClause.append(" AND (c_taxtype_ID =? or c_taxtype_ID is null)");
			params.add(charge.getC_TaxCategory_ID());
		}
		else if (M_Product_ID != 0)
		{
			MProduct product = new MProduct(ctx, M_Product_ID, null);
			whereClause.append(" AND (C_Taxtype_ID =? or C_TaxType_ID is null)");
			params.add(product.getC_TaxType_ID());
		}
		X_C_TaxDefinition taxdefinition = new Query(Env.getCtx(), X_C_TaxDefinition.Table_Name, whereClause.toString(), null)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(params)
			.setOrderBy("seqNo")
			.first();
		if (taxdefinition == null || taxdefinition.getC_Tax_ID() == 0) {
			return "";
		}
		else
			mTab.setValue("C_Tax_ID", new Integer(taxdefinition.getC_Tax_ID()));
		//
		if (steps) log.warning("fini");
		return "";
	}
	
	public String movementBPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		
		Integer C_BPartner_ID = (Integer)value;
		if (C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";
		
		X_C_BPartner bpartner = new X_C_BPartner(ctx, C_BPartner_ID, null);
		if (bpartner.getM_PriceList_ID() > 0)
			mTab.setValue("M_PriceList_ID", bpartner.getM_PriceList_ID());
		return "";
	}
	
	public String movementProduct (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		

		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0)
			return "";

		int C_BPartner_ID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		MProductPricing pp = new MProductPricing(M_Product_ID, C_BPartner_ID, Env.ONE, true, null);
				
		//
		int M_PriceList_ID = Env.getContextAsInt(ctx, WindowNo, "M_PriceList_ID");
		pp.setM_PriceList_ID(M_PriceList_ID);
		Timestamp orderDate = (Timestamp)mTab.getValue("MovementDate");
		/** PLV is only accurate if PL selected in header */
		int M_PriceList_Version_ID = Env.getContextAsInt(ctx, WindowNo, "M_PriceList_Version_ID");
		if ( M_PriceList_Version_ID == 0 && M_PriceList_ID > 0)
		{
			String sql = "SELECT plv.M_PriceList_Version_ID "
				+ "FROM M_PriceList_Version plv "
				+ "WHERE plv.M_PriceList_ID=? "						//	1
				+ " AND plv.ValidFrom <= ? "
				+ "ORDER BY plv.ValidFrom DESC";
			//	Use newest price list - may not be future
			
			M_PriceList_Version_ID = DB.getSQLValueEx(null, sql, M_PriceList_ID, orderDate);
			if ( M_PriceList_Version_ID > 0 )
				Env.setContext(ctx, WindowNo, "M_PriceList_Version_ID", M_PriceList_Version_ID );
		}
		pp.setM_PriceList_Version_ID(M_PriceList_Version_ID); 
		pp.setPriceDate(orderDate);
		//		
		mTab.setValue("PriceActual", pp.getPriceStd());
		return "";
	}
	
	
	
	
	
}	//	CalloutOrder


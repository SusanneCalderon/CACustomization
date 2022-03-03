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
/** Generated Model - DO NOT CHANGE */
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_TaxDeclarationType
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3 - $Id$ */
public class X_C_TaxDeclarationType extends PO implements I_C_TaxDeclarationType, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20210210L;

    /** Standard Constructor */
    public X_C_TaxDeclarationType (Properties ctx, int C_TaxDeclarationType_ID, String trxName)
    {
      super (ctx, C_TaxDeclarationType_ID, trxName);
      /** if (C_TaxDeclarationType_ID == 0)
        {
			setC_TaxDeclarationType_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_TaxDeclarationType (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_C_TaxDeclarationType[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set TaxDeclarationType ID.
		@param C_TaxDeclarationType_ID TaxDeclarationType ID	  */
	public void setC_TaxDeclarationType_ID (int C_TaxDeclarationType_ID)
	{
		if (C_TaxDeclarationType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_TaxDeclarationType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_TaxDeclarationType_ID, Integer.valueOf(C_TaxDeclarationType_ID));
	}

	/** Get TaxDeclarationType ID.
		@return TaxDeclarationType ID	  */
	public int getC_TaxDeclarationType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_TaxDeclarationType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}
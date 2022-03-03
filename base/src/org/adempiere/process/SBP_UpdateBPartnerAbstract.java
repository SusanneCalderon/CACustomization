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

import org.compiere.process.SvrProcess;

/** Generated Process for (SBP_BPartner_Update)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public abstract class SBP_UpdateBPartnerAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_BPartner_Update";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "SBP_BPartner_Update";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54225;
	/**	Parameter Name for Tax Group	*/
	public static final String C_TAXGROUP_ID = "C_TaxGroup_ID";
	/**	Parameter Name for Business Partner Group	*/
	public static final String C_BP_GROUP_ID = "C_BP_Group_ID";
	/**	Parameter Name for Document Type	*/
	public static final String C_DOCTYPE_ID = "C_DocType_ID";
	/**	Parameter Name for Purchase Order Doc Type	*/
	public static final String C_DOCTYPE_PO = "C_DocType_PO";
	/**	Parameter Value for Tax Group	*/
	private int taxGroupId;
	/**	Parameter Value for Business Partner Group	*/
	private int bPGroupId;
	/**	Parameter Value for Document Type	*/
	private int docTypeId;
	/**	Parameter Value for Purchase Order Doc Type	*/
	private int docTypePOId;

	@Override
	protected void prepare() {
		taxGroupId = getParameterAsInt(C_TAXGROUP_ID);
		bPGroupId = getParameterAsInt(C_BP_GROUP_ID);
		docTypeId = getParameterAsInt(C_DOCTYPE_ID);
		docTypePOId = getParameterAsInt(C_DOCTYPE_PO);
	}

	/**	 Getter Parameter Value for Tax Group	*/
	protected int getTaxGroupId() {
		return taxGroupId;
	}

	/**	 Setter Parameter Value for Tax Group	*/
	protected void setTaxGroupId(int taxGroupId) {
		this.taxGroupId = taxGroupId;
	}

	/**	 Getter Parameter Value for Business Partner Group	*/
	protected int getBPGroupId() {
		return bPGroupId;
	}

	/**	 Setter Parameter Value for Business Partner Group	*/
	protected void setBPGroupId(int bPGroupId) {
		this.bPGroupId = bPGroupId;
	}

	/**	 Getter Parameter Value for Document Type	*/
	protected int getDocTypeId() {
		return docTypeId;
	}

	/**	 Setter Parameter Value for Document Type	*/
	protected void setDocTypeId(int docTypeId) {
		this.docTypeId = docTypeId;
	}

	/**	 Getter Parameter Value for Purchase Order Doc Type	*/
	protected int getDocTypePOId() {
		return docTypePOId;
	}

	/**	 Setter Parameter Value for Purchase Order Doc Type	*/
	protected void setDocTypePOId(int docTypePOId) {
		this.docTypePOId = docTypePOId;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}
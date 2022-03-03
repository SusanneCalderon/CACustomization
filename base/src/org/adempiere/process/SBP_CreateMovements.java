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

import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductionLine;
import org.compiere.model.MStorage;

/** Generated Process for (SBP_CreateMovements)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class SBP_CreateMovements extends SBP_CreateMovementsAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		getSelectionKeys().stream().forEach(key ->{
			//
				MProductionLine productionLine = new MProductionLine(getCtx(), key, get_TrxName());
				MMovement movement = new MMovement (getCtx(), 0, get_TrxName());
				movement.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_MaterialMovement));
				movement.setAD_Org_ID(productionLine.getAD_Org_ID());
				movement.saveEx();
			//	To
			//	From: Look-up Storage
			MProduct product = MProduct.get(getCtx(), productionLine.getM_Product_ID());
			String MMPolicy = product.getMMPolicy();
			MStorage[] storages = MStorage.getWarehouse(getCtx(), getLocatorId(), 
					productionLine.getM_Product_ID(), 0, 
					null, MClient.MMPOLICY_FiFo.equals(MMPolicy), false, 0,  get_TrxName());
			BigDecimal target = getSelectionAsBigDecimal(productionLine.getM_ProductionLine_ID(), "PRL_QtyToDeliver");
			for (int j = 0; j < storages.length; j++)
			{
				MStorage storage = storages[j];
				if (storage.getQtyOnHand().signum() <= 0)
					continue;
				BigDecimal moveQty = target;
				if (storage.getQtyOnHand().compareTo(moveQty) < 0)
					moveQty = storage.getQtyOnHand();
				//
				MMovementLine movementLine = new MMovementLine(movement);
				movementLine.setM_Product_ID(productionLine.getM_Product_ID());
				movementLine.setMovementQty(moveQty);
				movementLine.setM_Locator_ID(storage.getM_Locator_ID());		//	from
				movementLine.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
				movementLine.setM_LocatorTo_ID(getLocatorId());					//	to
				movementLine.setM_AttributeSetInstanceTo_ID(storage.getM_AttributeSetInstance_ID());
				movementLine.saveEx();
				//
				target = target.subtract(moveQty);
				if (target.signum() == 0)
					break;
			}			
		});
		return "";
	}
}
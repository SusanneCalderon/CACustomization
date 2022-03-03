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
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MInOutConfirm;
import org.compiere.model.MInOutLineConfirm;
import org.compiere.model.MOrder;

/** Generated Process for (SBP_ConfirmCompleteAndSave)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.2
 */
public class SBP_ConfirmCompleteAndSave_1 extends SBP_ConfirmCompleteAndSaveAbstract
{

	protected List<MInOutConfirm> confirmsToComplete = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		confirmsToComplete = new ArrayList<MInOutConfirm>();
		getSelectionKeys().stream().forEach(key ->{
			MInOutLineConfirm  inOutLineConfirm = new MInOutLineConfirm(getCtx(), key, get_TrxName());
			BigDecimal target = getSelectionAsBigDecimal(inOutLineConfirm.getM_InOutLineConfirm_ID(), "IOLC_ConfirmedQty");
			if (target.compareTo(inOutLineConfirm.getTargetQty()) != 0)
				inOutLineConfirm.setConfirmedQty(target);
			inOutLineConfirm.saveEx();			
			Boolean isadded =  false;
			for (MInOutConfirm confirm:confirmsToComplete)
			{
				if (confirm.getM_InOutConfirm_ID() == inOutLineConfirm.getM_InOutConfirm_ID()) {
					isadded = true;
					break;					
				}
			}
			if (!isadded)
				confirmsToComplete.add((MInOutConfirm)inOutLineConfirm.getM_InOutConfirm());
		});
		for (MInOutConfirm confirm:confirmsToComplete) {
			confirm.processIt(MInOutConfirm.ACTION_Complete);
			confirm.saveEx();
		}
		return "";
	}
}
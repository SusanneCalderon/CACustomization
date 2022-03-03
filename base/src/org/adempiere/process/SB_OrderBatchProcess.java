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

import java.util.List;

import org.adempiere.util.ProcessUtil;
import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MPayment;
import org.compiere.model.MProcess;
import org.compiere.model.X_C_Order;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.X_HR_Process;

/** Generated Process for (C_Order_Process)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class SB_OrderBatchProcess extends SB_OrderBatchProcessAbstract
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
			MOrder order = new MOrder(getCtx(), key, get_TrxName());
			if (order.getDocStatus().equals("DR") && getDocAction().equals("CO"))
				completeProcess(order);
			else {
				order.setDocAction(getDocAction());
				order.processIt(getDocAction());
				order.saveEx();				
			}
		});
		return "";
	}
	
	private void completeProcess(MOrder order) {
		MProcess process = new MProcess(getCtx(), 104, get_TrxName());
		MPInstance instance = new MPInstance(Env.getCtx(), process.getAD_Process_ID(), order.getC_Order_ID());
		instance.saveEx();

		ProcessInfo pi = new ProcessInfo("Process_Order", 104);
		pi.setAD_PInstance_ID (instance.getAD_PInstance_ID());

		//	Add Parameter - Selection=Y
		//MPInstancePara ip = new MPInstancePara(instance, 10);
		//ip.setParameter(X_C_Order.COLUMNNAME_C_Order_ID, order.getC_Order_ID());
        //ip.saveEx();

		pi.setRecord_ID(order.getC_Order_ID());
		pi.setIsBatch(true);
		ProcessUtil.startWorkFlow(getCtx(), pi, process.getAD_Workflow_ID());
		//MWorkflow workflow = new MWorkflow(getCtx(), process.getAD_Workflow_ID(), get_TrxName());
		//workflow.start(pi, get_TrxName());
		
	}
}
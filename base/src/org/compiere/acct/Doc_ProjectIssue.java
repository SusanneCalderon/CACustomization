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
package org.compiere.acct;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MBPartner;
import org.compiere.model.MCostDetail;
import org.compiere.model.MProduct;
import org.compiere.model.MProject;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.ProductCost;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.X_HR_Concept_Acct;
/**
 *	Project Issue.
 *	Note:
 *		Will load the default GL Category. 
 *		Set up a document type to set the GL Category. 
 *	
 *  @author Jorg Janke
 *  @version $Id: Doc_ProjectIssue.java,v 1.2 2006/07/30 00:53:33 jjanke Exp $
 */
public class Doc_ProjectIssue extends Doc
{
	/**
	 *  Constructor
	 * 	@param ass accounting schemata
	 * 	@param rs record
	 * 	@param trxName trx
	 */
	public Doc_ProjectIssue (MAcctSchema[] ass, ResultSet rs, String trxName)
	{
		super (ass, MProjectIssue.class, rs, DOCTYPE_ProjectIssue, trxName);
	}   //  Doc_ProjectIssue
	private DocLine				m_line = null;
	private MProjectIssue		m_issue = null;

	/**
	 *  Load Document Details
	 *  @return error message or null
	 */
	protected String loadDocumentDetails()
	{
		setC_Currency_ID(NO_CURRENCY);
		m_issue = (MProjectIssue)getPO();
		setDateDoc (m_issue.getMovementDate());
		setDateAcct(m_issue.getMovementDate());
			
		//	Pseudo Line
		m_line = new DocLine (m_issue, this); 
		m_line.setQty (m_issue.getMovementQty(), true);    //  sets Trx and Storage Qty
		m_line.setReversalLine_ID(m_issue.getReversalLine_ID());	
		
		//	Pseudo Line Check
		if (m_line.getM_Product_ID() == 0)
			log.warning(m_line.toString() + " - No Product");
		log.fine(m_line.toString());
		
		return null;
	}   //  loadDocumentDetails

	/**
	 * 	Get DocumentNo
	 *	@return document no
	 */
	public String getDocumentNo ()
	{
		MProject p = m_issue.getParent();
		if (p != null)
			return p.getValue() + " #" + m_issue.getLine();
		return "(" + m_issue.get_ID() + ")";
	}	//	getDocumentNo

	/**
	 *  Get Balance
	 *  @return Zero (always balanced)
	 */
	public BigDecimal getBalance()
	{
		BigDecimal retValue = Env.ZERO;
		return retValue;
	}   //  getBalance

	/**
	 *  Create Facts (the accounting logic) for
	 *  PJI
	 *  <pre>
	 *  Issue
	 *      ProjectWIP      DR
	 *      Inventory               CR
	 *  </pre>
	 *  Project Account is either Asset or WIP depending on Project Type
	 *  @param as accounting schema
	 *  @return Fact
	 */
	public ArrayList<Fact> createFacts (MAcctSchema as)
	{
		//  create Fact Header
		ArrayList<Fact> facts = new ArrayList<Fact>();
		Fact fact = new Fact(this, as, Fact.POST_Actual);
		setC_Currency_ID (as.getC_Currency_ID());

		MProject project = new MProject (getCtx(), m_issue.getC_Project_ID(), getTrxName());
		String ProjectCategory = project.getProjectCategory();
		MProduct product = MProduct.get(getCtx(), m_issue.getM_Product_ID());
			
		//  Line pointers
		FactLine debitLine = null;
		FactLine creditLine = null;

		//  Issue Cost
		BigDecimal costs = null;
		BigDecimal total = Env.ZERO;
		if (m_issue.getM_InOutLine_ID() != 0) {

			/*
			 * for (MCostDetail cost : m_line.getCostDetail(as, true)) { if
			 * (!MCostDetail.existsCost(cost)) continue;
			 * 
			 * costs = MCostDetail.getTotalCost(cost, as); total = total.add(costs); }
			 */
			costs = getPOCost(as).multiply(m_issue.getMovementQty());
			total = costs;
		}
		else if (m_issue.getS_TimeExpenseLine_ID() != 0) {

			MTimeExpenseLine timeExpenseLine = new  MTimeExpenseLine(getCtx(), m_issue.getS_TimeExpenseLine_ID(), getTrxName());
			if (timeExpenseLine.get_ValueAsInt("HR_Concept_ID")> 0)
				return facts;
			costs = getLaborCost(as);
			total = costs;

			//  Project         DR
			int acctType = ACCTTYPE_ProjectWIP;
			if (MProject.PROJECTCATEGORY_AssetProject.equals(ProjectCategory))
				acctType = ACCTTYPE_ProjectAsset;
			MAccount debitAccount = getAccount(acctType, as);
			/*MHRConcept concept = MHRConcept.getById(as.getCtx(), timeExpenseLine.get_ValueAsInt("HR_Concept_ID") , getTrxName());
			//	Get Concept Account

			X_HR_Concept_Acct conceptAcct = concept.getConceptAcct(
					Optional.ofNullable(as.getC_AcctSchema_ID()),
					Optional.ofNullable(null),
					Optional.ofNullable(timeExpenseLine.getC_BPartner().getC_BP_Group_ID()));*/
			//MAccount accountBPD = MAccount.getValidCombination (getCtx(), conceptAcct.getHR_Expense_Acct() , getTrxName());
			debitLine = fact.createLine(m_line, debitAccount, as.getC_Currency_ID(),costs, null);
			debitLine.setDescription(timeExpenseLine.getM_Product().getName() );
			debitLine.saveEx();
			acctType = ProductCost.ACCTTYPE_P_Asset;
			if (product.isService())
				acctType = ProductCost.ACCTTYPE_P_Expense;
			creditLine = fact.createLine(m_line,
				m_line.getAccount(acctType,as),
				as.getC_Currency_ID(), null, costs);
			creditLine.setM_Locator_ID(m_line.getM_Locator_ID());
			creditLine.setLocationFromLocator(m_line.getM_Locator_ID(), true);	// from Loc
			creditLine.setDescription(timeExpenseLine.getM_Product().getName() );
			creditLine.saveEx();
			facts.add(fact);
			return facts;
		
		}
		if (costs == null)	//	standard Product Costs
		{	
			for(MCostDetail cost : m_line.getCostDetail(as, false))
			{	
				if(!MCostDetail.existsCost(cost))
					continue;
				
				costs = MCostDetail.getTotalCost(cost, as);
				total = total.add(costs);
			}	
		}	
		
		if (total == null || total.signum() == 0)
		{
			p_Error = "Resubmit - No Costs for " + product.getName();
			log.log(Level.WARNING, p_Error);
			return null;
		}
		
		//  Project         DR
		int acctType = ACCTTYPE_ProjectWIP;
		if (MProject.PROJECTCATEGORY_AssetProject.equals(ProjectCategory))
			acctType = ACCTTYPE_ProjectAsset;
		debitLine = fact.createLine(m_line,
			getAccount(acctType, as), as.getC_Currency_ID(), total, null);
		debitLine.setQty(m_line.getQty().negate());
		
		//  Inventory               CR
		acctType = ProductCost.ACCTTYPE_P_Asset;
		if (product.isService())
			acctType = ProductCost.ACCTTYPE_P_Expense;
		creditLine = fact.createLine(m_line,
			m_line.getAccount(acctType,as),
			as.getC_Currency_ID(), null, total);
		creditLine.setM_Locator_ID(m_line.getM_Locator_ID());
		creditLine.setLocationFromLocator(m_line.getM_Locator_ID(), true);	// from Loc
		//
		facts.add(fact);
		return facts;
	}   //  createFact

	/**
	 * 	Get PO Costs in Currency of AcctSchema
	 *	@param as Account Schema
	 *	@return Unit PO Cost
	 */
	private BigDecimal getPOCost(MAcctSchema as)
	{
		BigDecimal retValue = null;
		//	Uses PO Date
		String sql = "SELECT currencyConvert(ol.PriceActual, o.C_Currency_ID, ?, o.DateOrdered, o.C_ConversionType_ID, ?, ?) "
			+ "FROM C_OrderLine ol"
			+ " INNER JOIN M_InOutLine iol ON (iol.C_OrderLine_ID=ol.C_OrderLine_ID)"
			+ " INNER JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID) "
			+ "WHERE iol.M_InOutLine_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, getTrxName());
			pstmt.setInt(1, as.getC_Currency_ID());
			pstmt.setInt(2, getAD_Client_ID());
			pstmt.setInt(3, getAD_Org_ID());
			pstmt.setInt(4, m_issue.getM_InOutLine_ID());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				retValue = rs.getBigDecimal(1);
				log.fine("POCost = " + retValue);
			}
			else
				log.warning("Not found for M_InOutLine_ID=" + m_issue.getM_InOutLine_ID());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			pstmt = null; rs = null;
		}
		return retValue;
	}	//	getPOCost();

	/**
	 * 	Get Labor Cost from Expense Report
	 *	@param as Account Schema
	 *	@return Unit Labor Cost
	 */

	private BigDecimal getLaborCost(MAcctSchema as)
	{
		// Todor Lulov 30.01.2008		
		BigDecimal retValue = Env.ZERO;
		BigDecimal qty = Env.ZERO;
		if (m_issue.getS_TimeExpenseLine_ID() > 0) {
			return Env.ZERO;
		}
		String sql = "SELECT ConvertedAmt, Qty FROM S_TimeExpenseLine " + 
			  " WHERE S_TimeExpenseLine.S_TimeExpenseLine_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString (), getTrxName());
			pstmt.setInt(1, m_issue.getS_TimeExpenseLine_ID());	
			rs = pstmt.executeQuery();			
			if (rs.next())
			{
				retValue = rs.getBigDecimal(1);
				qty = rs.getBigDecimal(2);
				retValue = retValue.multiply(m_issue.getMovementQty()); 
				log.fine("ExpLineCost = " + retValue);
			}
			else
				log.warning("Not found for S_TimeExpenseLine_ID=" + m_issue.getS_TimeExpenseLine_ID());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			pstmt = null; rs = null;
		}		
		return retValue;
	}	//	getLaborCost
	
	private X_HR_Concept_Acct getConceptAcct(int conceptID,MAcctSchema as, MBPartner bpartner) {

		MHRConcept concept = MHRConcept.getById(getCtx(),conceptID , getTrxName());
		//	Get Concept Account
		X_HR_Concept_Acct conceptAcct = concept.getConceptAcct(
				Optional.ofNullable(as.getC_AcctSchema_ID()),
				Optional.ofNullable(null),
				Optional.ofNullable(bpartner.getC_BP_Group_ID()));

		return conceptAcct;
	}

}	//	DocProjectIssue
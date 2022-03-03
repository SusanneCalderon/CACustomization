package org.adempiere.process;

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProduct;
import org.compiere.process.DocAction;

/** Generated Process for (SBP_GenerateMovementFromStorage)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class SBP_GenerateMovementFromStorage extends SBP_GenerateMovementFromStorageAbstract
{
	private MMovement movement = null;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{

		
		
		for(Integer key : getSelectionKeys()) {

			int locatorID = getSelectionAsInt(key, "S_M_Locator_ID");
			if (movement == null) {
				movement  = new MMovement(getCtx(), 0, get_TrxName());
				MLocator locator = new MLocator(getCtx(), locatorID, get_TrxName());
				movement.setAD_Org_ID(locator.getAD_Org_ID());
				movement.setIsInTransit(false);
				//Look the document type based on organization
				
				int docTypeId = getDocTypeId()>0?
						getDocTypeId():
						MDocType.getDocType(MDocType.DOCBASETYPE_MaterialMovement, movement.getAD_Org_ID());

				if (docTypeId > 0)
					movement.setC_DocType_ID(docTypeId);
				movement.saveEx();
				addLog(movement.get_ID(), movement.getMovementDate(),BigDecimal.ZERO , movement.getDocumentInfo());
				setCurrentMovement(movement);
			}
			//	get values from result set
			int productID =getSelectionAsInt(key, "S_M_Product_ID");
			BigDecimal movementQty = getSelectionAsBigDecimal(key, "S_MovementQty");
			int asiID = getSelectionAsInt(key, "S_M_AttributeSetInstance_ID");
			BigDecimal QtyOnHand = getSelectionAsBigDecimal(key, "S_QtyOnHand");
			if (movementQty.compareTo(QtyOnHand)>0)
				return "@QtyAvailable@ < @MovementQty@";
			createLine(movement, productID, locatorID, asiID, movementQty);			
			}
		if (!movement.processIt(MMovement.ACTION_Complete))
			throw new AdempiereException("@Error@ " + movement.getProcessMsg()); 
		return movement.getDocumentNo();
	}
	
	private void createLine(MMovement mMovement, int productID, int locatorID, int asiID, BigDecimal movementQty) {
		MMovementLine movementLine = new MMovementLine(movement);
		movementLine.setM_Product_ID(productID);
		if (asiID > 0)
			movementLine.setM_AttributeSetInstance_ID(asiID);
		movementLine.setMovementQty(movementQty);
		movementLine.setM_Locator_ID(locatorID);
		movementLine.setM_LocatorTo_ID(getLocatorId());
		movementLine.saveEx();
	}
	
	private Optional<MMovement> getCurrentMovement()
    {
        return Optional.ofNullable(movement);
    }

    private void setCurrentMovement(MMovement movement)
    {
        this.movement = movement;
    }
}
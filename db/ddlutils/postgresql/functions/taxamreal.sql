CREATE OR REPLACE FUNCTION taxamtreal(p_c_orderline_id numeric)
  RETURNS numeric AS
$BODY$

DECLARE
          v_taxamt numeric;  
BEGIN

select case when pl.istaxincluded = 'Y' AND t.rate <> 0 
	then 
	   case 
		when o.docstatus in ('CL')
		then round(((ol.qtyinvoiced * ol.priceactual)- (ol.qtyinvoiced * ol.priceactual)/(1+(t.rate/100))), cur.stdprecision) 
		else round((ol.linenetamt- ol.linenetamt/(1+(t.rate/100))), cur.stdprecision) 
            end 
         else 0 end into v_taxamt 

	from c_Orderline ol 
	inner join c_order o on ol.c_order_ID = o.c_order_ID 
	inner join m_pricelist pl on o.m_pricelist_ID = pl.m_pricelist_ID 
	inner join c_tax t on ol.c_tax_ID = t.c_tax_ID 
	inner join c_currency cur on o.c_currency_ID = cur.c_Currency_ID 
	where ol.c_orderline_ID=p_c_orderLine_ID;

    RETURN coalesce(v_taxamt,0);

END;

$BODY$
  LANGUAGE plpgsql;

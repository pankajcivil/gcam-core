/*
 * LEGAL NOTICE
 * This computer software was prepared by Battelle Memorial Institute,
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
 * with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
 * CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 * 
 * EXPORT CONTROL
 * User agrees that the Software will not be shipped, transferred or
 * exported into any country or used in any manner prohibited by the
 * United States Export Administration Act or any other applicable
 * export laws, restrictions or regulations (collectively the "Export Laws").
 * Export of the Software may require some form of license or other
 * authority from the U.S. Government, and failure to obtain such
 * export control license may result in criminal liability under
 * U.S. laws. In addition, if the Software is identified as export controlled
 * items under the Export Laws, User represents and warrants that User
 * is not a citizen, or otherwise located within, an embargoed nation
 * (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
 *     and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 * 
 * All rights to use the Software are granted on condition that such
 * rights are forfeited if User fails to comply with the terms of
 * this Agreement.
 * 
 * User agrees to identify, defend and hold harmless BATTELLE,
 * its officers, agents and employees from all liability involving
 * the violation of such Export Laws, either directly or indirectly,
 * by User.
 */

/*! 
 * \file co2_emissions.h
 * \ingroup Objects
 * \brief CO2Emissions class header file.
 * \author Jim Naslund
 */

#include "util/base/include/definitions.h"
//#include "emissions/include/aghg.h"

#include "emissions/include/co2_emissions.h"
#include "containers/include/scenario.h"
#include "marketplace/include/marketplace.h"
#include "util/base/include/xml_helper.h"
#include "containers/include/iinfo.h"
#include "technologies/include/ioutput.h"
#include "technologies/include/icapture_component.h"

using namespace std;
using namespace xercesc;


extern Scenario* scenario;

//! Default Constructor with default emissions unit and name.
CO2Emissions::CO2Emissions(): mName("CO2"){
    mEmissionsUnit = "MTC";
}

//! Default Destructor.
CO2Emissions::~CO2Emissions()
{
}

//! Clone operator.
CO2Emissions* CO2Emissions::clone() const {
    return new CO2Emissions( *this );
}

void CO2Emissions::copyGHGParameters( const AGHG* aPrevGHG ){
    // Nothing needs to be copied.
}

/*!
 * \brief Get the XML node name for output to XML.
 * \details This public function accesses the private constant string, XML_NAME.
 *          This way the tag is always consistent for both read-in and output and can be easily changed.
 *          This function may be virtual to be overridden by derived class pointers.
 * \author Jim Naslund
 * \return The constant XML_NAME.
 */
const string& CO2Emissions::getXMLName() const {
    return getXMLNameStatic();
}

const string& CO2Emissions::getXMLNameStatic(){
    static const string XML_NAME = "CO2";
    return XML_NAME;
}

/*!
 * \brief Set the name of the CO2 object.
 * \author Sonny Kim
 */
void CO2Emissions::parseName( const string& aNameAttr ){
    mName = aNameAttr;
}

/*!
 * \brief Get the name of the CO2 object.
 * \details This public function accesses the private string mName.
 *          The CO2 object name can be different from "CO2" in order to
 *          allow specific technology / sector carbon policies.
 * \author Sonny Kim
 * \return The unique name of CO2 gas.
 */
const string& CO2Emissions::getName() const {
    return mName;
}


bool CO2Emissions::XMLDerivedClassParse( const string& nodeName, const DOMNode* curr ){
    return false;
}

void CO2Emissions::toInputXMLDerived( ostream& out, Tabs* tabs ) const {
    // write the xml for the class members.
    XMLWriteElementCheckDefault( mEmissionsUnit, "emissions-unit", out, tabs, string("MTC") );
}

void CO2Emissions::toDebugXMLDerived( const int period, ostream& out, Tabs* tabs ) const {
    // write the xml for the class members.
    XMLWriteElement( mEmissionsUnit, "emissions-unit", out, tabs );
}

void CO2Emissions::initCalc( const string& aRegionName,
                             const IInfo* aLocalInfo,
                             const int aPeriod )
{
}

double CO2Emissions::getGHGValue( const std::string& aRegionName,
                                const std::vector<IInput*>& aInputs,
                                const std::vector<IOutput*>& aOutputs,
                                const ICaptureComponent* aSequestrationDevice,
                                const int aPeriod ) const
{
    // Constants
    const double CVRT90 = 2.212; // 1975 $ to 1990 $
    // Conversion from teragrams of carbon per EJ to metric tons of carbon per GJ
    const double CVRT_TG_MT = 1e-3; 

    // Get carbon storage cost from the sequestrion device if there is one.
    double storageCost = aSequestrationDevice ? 
        aSequestrationDevice->getStorageCost( aRegionName, getName(), aPeriod ) : 0;

    // Get the remove fraction from the sequestration device. The remove
    // fraction is zero if there is no sequestration device.
    double removeFraction = aSequestrationDevice ? aSequestrationDevice->getRemoveFraction( getName() ) : 0;

    // Get the greenhouse gas tax from the marketplace.
    const Marketplace* marketplace = scenario->getMarketplace();
    double GHGTax = marketplace->getPrice( getName(), aRegionName, aPeriod, false );

    if( GHGTax == Marketplace::NO_MARKET_PRICE ){
        GHGTax = 0;
    }
    
    // Retrieve proportional tax rate.
    const IInfo* marketInfo = marketplace->getMarketInfo( getName(), aRegionName, aPeriod, false );
    // Note: the key includes the region name.
    const double proportionalTaxRate = 
        ( marketInfo && marketInfo->hasValue( "proportional-tax-rate" + aRegionName ) ) 
        ? marketInfo->getDouble( "proportional-tax-rate" + aRegionName, true )
        : 1.0;
    // Adjust greenhouse gas tax with the proportional tax rate.
    GHGTax *= proportionalTaxRate;

    // get the summation of emissions coefficients from all outputs
    // SHK 3/15/07: is this correct?
    double coefProduct = calcOutputCoef( aOutputs, aPeriod );
    double coefInput = calcInputCoef( aInputs, aPeriod );

    // Calculate the generalized emissions cost per unit.
    // units for generalized cost is in 1975$/GJ
    double generalizedCost = ( ( 1 - removeFraction ) * GHGTax + removeFraction * storageCost )
            * ( coefInput - coefProduct) / CVRT90 * CVRT_TG_MT;

    return generalizedCost;
}

void CO2Emissions::calcEmission( const std::string& aRegionName, 
                               const std::vector<IInput*>& aInputs,
                               const std::vector<IOutput*>& aOutputs,
                               const GDP* aGDP,
                               ICaptureComponent* aSequestrationDevice,
                               const int aPeriod )
{
    // Calculate the aggregate emissions of all inputs.
    double inputEmissions = calcInputCO2Emissions( aInputs, aRegionName, aPeriod );
    double outputEmissions = calcOutputEmissions( aOutputs, aPeriod );

    // Calculate sequestered emissions if there is a sequestration device
    // and subtract from input emissions.
    if( aSequestrationDevice ){
        mEmissionsSequestered[ aPeriod ] = aSequestrationDevice->calcSequesteredAmount( 
                                           aRegionName, getName(), inputEmissions, aPeriod );

        inputEmissions -= mEmissionsSequestered[ aPeriod ];
    }

    double totalEmissions = inputEmissions;

    // If the good is a primary fuel, don't subtract output emissions as this is
    // extraction of the resource, not sequestration, and store the output
    // emissions as emissions by primary fuel. This should probably be stored by
    // the GHG.
    // This is wrong if capture occurs down the line.
    // TODO: Store this property in the output object.
    Marketplace* marketplace = scenario->getMarketplace();
    const IInfo* marketInfo = marketplace->getMarketInfo( aOutputs[ 0 ]->getName(),
        aRegionName, aPeriod, false );
    if( marketInfo && marketInfo->getBoolean( "IsPrimaryEnergyGood", false ) ){
        mEmissionsByFuel[ aPeriod ] = outputEmissions;
    }
    else {
        // Remove emissions contained in the output from the total technology emissions.
        totalEmissions -= outputEmissions;
    }

    // Store the total emissions.
    mEmissions[ aPeriod ] = totalEmissions;

    addEmissionsToMarket( aRegionName, aPeriod );
}
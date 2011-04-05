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
* \file atom.cpp
* \ingroup Util
* \brief objects::Atom class source file.
* \author Josh Lurz
*/

#include "util/base/include/definitions.h"
#include <string>
#include <iostream>
#include "util/base/include/atom.h"
#include "util/base/include/atom_registry.h"
#include <boost/functional/hash/hash.hpp>

using namespace std;

namespace objects {
	/*! \brief Constructor which registers the Atom with the AtomRegistry so that it
	*          can be located from throughout the model and deallocated
	*          automatically. 
	*/
	Atom::Atom( const string& aUniqueID ):mUniqueID( aUniqueID ){
		// Register the atom. The registry checks for uniqueness and handles
		// deallocation.
		AtomRegistry::getInstance()->registerAtom( this );

		// Compute the hash used for storage of atoms within maps.
		boost::hash<std::string> hashFunction;
		mHashCode = hashFunction( aUniqueID );
	}

	/*! \brief Destructor
	* \details The destructor checks if the AtomRegistry is currently deallocating
	*          Atoms, and if prints a warning if it is not. This would mean that the
	*          Atom was allocated incorrectly. 
	*/
	Atom::~Atom(){
		if( !AtomRegistry::getInstance()->isCurrentlyDeallocating() ){
			cout << "Atom is being deallocated before the AtomRegistry deleted it." << endl;
		}
	}
}

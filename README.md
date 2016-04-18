# Auto-WEKA for MCPS
Auto-WEKA is a tool for automating the selection of methods and hyperparameters of WEKA. This repository contains an extended version of Auto-WEKA now supporting the optimisation of MultiComponent Predictive Systems (MCPS).

## Description
Many different machine learning algorithms exist that can easily be used off the shelf, many of these methods are implemented in the open source WEKA package. However, each of these algorithms have their own hyperparameters that can drastically change their performance, and there are a staggeringly large number of possible alternatives overall. Auto-WEKA considers the problem of simultaneously composing an MCPS and setting its hyperparameters, going beyond previous methods that address these issues in isolation. Auto-WEKA does this using a fully automated approach, leveraging recent innovations in Bayesian optimization. Our hope is that Auto-WEKA will help non-expert users to more effectively identify machine learning algorithms and hyperparameter settings appropriate to their applications, and hence to achieve improved performance.

## Publications
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. "Automatic composition and optimisation of multicomponent predictive systems" Submitted to IEEE Transactions on Knowledge and Data Engineering on April 1st, 2016. ([Under revision](https://www.dropbox.com/s/cd13qr1touqgl9z/Automatic_composition_and_optimisation_of_multicomponent_predictive_systems%20-%20prereview.pdf?dl=0))
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. ["Towards automatic composition of multicomponent predictive systems"](http://link.springer.com/chapter/10.1007%2F978-3-319-32034-2_3) In Proc. of HAIS 2016, 2016. 
 * Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown. ["Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms"](https://dl.acm.org/citation.cfm?id=2487629) In Proc. of KDD 2013, 2013.

## Authors of this Auto-WEKA extension
 * Manuel Martin Salvador, PhD Candidate (Bournemouth University)
 * Marcin Budka, Senior Lecturer (Bournemouth University)
 * Bogdan Gabrys, Professor (Bournemouth University)
 
## Original authors of Auto-WEKA
 * Chris Thornton, M.Sc. Student (UBC)
 * Frank Hutter, Assistant Professor (Freiburg University)
 * Holger Hoos, Professor (UBC)
 * Kevin Leyton-Brown, Associate Professor (UBC)
 
## Notes
This version was developed using as base Auto-WEKA 0.5 from http://www.cs.ubc.ca/labs/beta/Projects/autoweka/

## License
GNU General Public License v3 (see [LICENSE](https://github.com/DraXus/autoweka/blob/master/LICENSE))

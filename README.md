# Auto-WEKA for MCPS
Auto-WEKA is a tool for automating the selection of methods and hyperparameters of WEKA. This repository contains an extended version of Auto-WEKA now supporting the optimisation of MultiComponent Predictive Systems (MCPS).

## Description
Many different machine learning algorithms exist that can easily be used off the shelf, many of these methods are implemented in the open source WEKA package. However, each of these algorithms have their own hyperparameters that can drastically change their performance, and there are a staggeringly large number of possible alternatives overall. Auto-WEKA considers the problem of simultaneously composing an MCPS and setting its hyperparameters, going beyond previous methods that address these issues in isolation. Auto-WEKA does this using a fully automated approach, leveraging recent innovations in Bayesian optimization. Our hope is that Auto-WEKA will help non-expert users to more effectively identify machine learning algorithms and hyperparameter settings appropriate to their applications, and hence to achieve improved performance.

## Papers
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. "Automatic composition and optimisation of multicomponent predictive systems" Submitted to IEEE Transactions on Knowledge and Data Engineering. [Under revision]
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. "Towards automatic composition of multicomponent predictive systems" In Proc. of HAIS 2016, 2016. [Accepted]
 * Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown. "Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms" In Proc. of KDD 2013, 2013. [The main Auto-WEKA Paper]

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

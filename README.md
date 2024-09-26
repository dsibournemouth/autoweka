# Auto-WEKA for MCPS (AutoWeka4MCPS)
Auto-WEKA is a tool for automating the selection of methods and hyperparameters of WEKA. This repository contains an extended version of Auto-WEKA now supporting the optimisation of MultiComponent Predictive Systems (MCPS).

![GUI Main](https://raw.githubusercontent.com/dsibournemouth/autoweka/master/img/GUI-main.png)

## Description
Many different machine learning algorithms exist that can easily be used off the shelf, many of these methods are implemented in the open source WEKA package. However, each of these algorithms have their own hyperparameters that can drastically change their performance, and there are a staggeringly large number of possible alternatives overall. Auto-WEKA considers the problem of simultaneously composing an MCPS and setting its hyperparameters, going beyond previous methods that address these issues in isolation. Auto-WEKA does this using a fully automated approach, leveraging recent innovations in Bayesian optimization. Our hope is that Auto-WEKA will help non-expert users to more effectively identify machine learning algorithms and hyperparameter settings appropriate to their applications, and hence to achieve improved performance.

## Auto-WEKA as a blackbox
Auto-WEKA includes a wizard to find the best MCPS of a given dataset without any user interaction, apart from providing a dataset and a time budget.
![GUI Wizard](https://raw.githubusercontent.com/dsibournemouth/autoweka/master/img/GUI-wizard.png)

## Auto-WEKA as an advanced toolbox
Auto-WEKA allows to select between a list of all WEKA methods and different optimisation strategies and parameters.
![GUI Builder](https://raw.githubusercontent.com/dsibournemouth/autoweka/master/img/GUI-builder.png)

## Usage 

* Please watch the [video tutorial](https://github.com/UTS-AAi/autoweka/blob/master/autoweka4mcps/autoweka4mcps_quick_start_guide_ui.mp4) for quick start guide with the user interface (autoweka4mcps/autoweka4mcps_quick_start_guide_ui.mp4)
* It's neccessary to allocate more memory for AutoWeka4MCPS to deal with large datasets, e.g., allocating 2GB memory:
```
cd autoweka4mcps
java -Xmx2048m -jar autoweka4mcps.jar
```

* SMAC Configuration: Please configure the file path to SMAC in the following file autoweka.smac.SMACExperimentConstructor.properties

* The experiments folders have to be put in the parent folder of AutoWeka4MCPS (default)



## Publications
* Tien-Dung Nguyen, David Jacob Kedziora, Katarzyna Musial, and Bogdan Gabrys. ["Exploring Opportunistic Meta-knowledge to Reduce Search Spaces for Automated Machine Learning."](https://arxiv.org/pdf/2105.00282.pdf). In The International Joint Conference on Neural Network IJCNN. IEEE, 2021. (Accepted)
* David Jacob Kedziora, Katarzyna Musial, and Bogdan Gabrys. ["AutonoML: Towards an Integrated Framework for Autonomous Machine Learning."](https://arxiv.org/pdf/2012.12600) arXiv preprint arXiv:2012.12600 (2020). (Under Review)
* Tien-Dung Nguyen, Bogdan Gabrys, and Katarzyna Musial. ["AutoWeka4MCPS-AVATAR: Accelerating Automated Machine Learning Pipeline Composition and Optimisation."](https://arxiv.org/abs/2011.11846) arXiv preprint arXiv:2011.11846. 2020. (Under Review)
* Tien-Dung Nguyen, Tomasz Maszczyk, Katarzyna Musial, Marc-Andre Zöller, and Bogdan Gabrys. ["AVATAR-Machine Learning Pipeline Evaluation Using Surrogate Model"](https://link.springer.com/chapter/10.1007/978-3-030-44584-3_28). In International Symposium on Intelligent Data Analysis, pp. 352-365. Springer, Cham, 2020.
* Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. "Automatic composition and optimisation of multicomponent predictive systems with an extended Auto-WEKA" Submitted to IEEE Transactions on Automation Science and Engineering, 2018. [[slides](http://www.slideshare.net/draxus/automating-machine-learning-is-it-feasible-62661182)] [[results](https://github.com/dsibournemouth/autoweka/tree/master/results)]
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. "Modelling Multi-Component Predictive Systems as Petri Nets". Submitted to 15th Annual Industrial Simulation Conference, 2017 (under review). [[branch](https://github.com/dsibournemouth/autoweka/tree/feature/pnml)]
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. ["Effects of change propagation resulting from adaptive preprocessing in multicomponent predictive systems"](http://www.sciencedirect.com/science/article/pii/S187705091632066X). In Proc. of the 20th International Conference KES-2016.
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. ["Adapting Multicomponent Predictive Systems using Hybrid Adaptation Strategies with Auto-WEKA in Process Industry"](http://www.jmlr.org/proceedings/papers/v64/salvador_adapting_2016.pdf). In Proc. of the 2016 Workshop on Automatic Machine Learning at ICML 2016. [[branch](https://github.com/dsibournemouth/autoweka/tree/feature/batch-adaptation)] [[results](https://github.com/dsibournemouth/autoweka/tree/feature/batch-adaptation/results)]  
 * Manuel Martin Salvador, Marcin Budka, and Bogdan Gabrys. ["Towards automatic composition of multicomponent predictive systems"](http://link.springer.com/chapter/10.1007%2F978-3-319-32034-2_3) In Proc. of HAIS 2016, 2016. [[slides](http://www.slideshare.net/draxus/towards-automatic-composition-of-multicomponent-predictive-systems)] [[results](https://github.com/dsibournemouth/autoweka/tree/master/results)]
 * Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown. ["Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms"](https://dl.acm.org/citation.cfm?id=2487629) In Proc. of KDD 2013, 2013.

# The Extension of Auto-WEKA for MCPS with the AVATAR
 * This version of Auto-WEKA is extended by integrating the AVATAR (https://github.com/UTS-AAi/AVATAR) . The AVATAR evaluates the validity of ML pipelines using a surrogate model that enables
to accelerate automatic ML pipeline composition and optimisation.

## Authors of this Auto-WEKA extension
 * [Tien-Dung Nguyen] (https://www.linkedin.com/in/tien-dung-nguyen-29bb42170/), University of Technology Sydney, Australia
 * [Professor Bogdan Gabrys] (http://bogdan-gabrys.com/), University of Technology Sydney, Australia
 * [Associate Professor Katarzyna Musial Gabrys] (http://www.katarzyna-musial.com/), University of Technology Sydney, Australia
 * [Tomasz.Maszczyk], University of Technology Sydney, Australia
 * [Marc-André Zöller] (https://www.researchgate.net/profile/Marc_Andre_Zoeller), USU Software AG, Germany
 * [Simon Kocbek] (https://www.uts.edu.au/staff/simon.kocbek), University of Technology Sydney, Australia

## Authors of the original Auto-Weka for MCPS
 * [Manuel Martin Salvador](http://staffprofiles.bournemouth.ac.uk/display/msalvador), PhD Candidate (Bournemouth University)
 * [Marcin Budka](http://staffprofiles.bournemouth.ac.uk/display/mbudka), Senior Lecturer (Bournemouth University)
 * [Bogdan Gabrys](http://bogdan-gabrys.com), Professor (Bournemouth University)
 
## Original authors of Auto-WEKA
 * [Chris Thornton](http://www.cs.ubc.ca/~cwthornt/), M.Sc. Student (UBC)
 * [Frank Hutter](http://www2.informatik.uni-freiburg.de/~hutter/index.html), Assistant Professor (Freiburg University)
 * [Holger Hoos](http://www.cs.ubc.ca/~hoos/), Professor (UBC)
 * [Kevin Leyton-Brown](http://www.cs.ubc.ca/~kevinlb/), Associate Professor (UBC)
 
## Notes
This version was developed using as base Auto-WEKA 0.5 from http://www.cs.ubc.ca/labs/beta/Projects/autoweka/

## Disclaimer
This software is intended for research purposes and not recommended for production environments. Support is not guaranteed, but please [contact us](https://github.com/dsibournemouth/autoweka/issues) if you have any question or would like to collaborate.

## License
GNU General Public License v3 (see [LICENSE](https://github.com/DraXus/autoweka/blob/master/LICENSE))

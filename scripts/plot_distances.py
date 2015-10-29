import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
from config import datasets

def plot_distances(x, y, labels):
    print "----"
    print labels
    print x
    print y
    print "----"
    
    fig, ax = plt.subplots()
    colors = np.random.rand(21) # 21 datasets
    dataset_dict = dict()
    markers = dict()
    markers['RAND'] = '>'
    markers['SMAC'] = 'o'
    markers['TPE'] = (5,1)
    
    # shape by strategy 
    # color by dataset
    color_index = 0
    for p in range(0,len(x)):
        tmp = labels[p].split('.')
        dataset = tmp[0]
        
        dataset_index = datasets.index(dataset)
        color = cm.hsv(dataset_index / 25., 1)
            
        strategy = tmp[1]
        plt.plot(x[p], y[p], marker=markers[strategy], c=color, alpha=.75, label=labels[p])
    
    #plt.tight_layout()
    plt.title('error variance vs configuration dissimilarity')
    plt.xlabel('MCPS dissimilarity')
    plt.ylabel('Error variance')
    # TODO fix legend
    handles, labels = ax.get_legend_handles_labels()
    lgd = ax.legend(handles, labels, loc='center right', bbox_to_anchor=(0.5,-0.1), numpoints=1)
    plt.savefig('../distances/_all.png', dpi=200, bbox_extra_artists=(lgd,), bbox_inches='tight')
    plt.close("all")



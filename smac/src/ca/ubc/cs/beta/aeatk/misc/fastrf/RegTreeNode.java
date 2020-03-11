package ca.ubc.cs.beta.aeatk.misc.fastrf;
//
//
//import java.awt.Color;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Set;
//
//import org.StructureGraphic.v1.DSTreeNode;
//
//import ca.ubc.cs.beta.models.fastrf.Regtree;
//import ca.ubc.cs.beta.models.fastrf.RegtreeFwd;
//
//public class RegTreeNode implements DSTreeNode {
//
//	private final  Regtree tree;
//	
//	private final int index;
//
//	private final ParamConfigurationSpace configSpace;
//
//	private final List<ProblemInstance> instances;
//	
//	private final ParamConfiguration incumbent;
//
//	private final Set<Integer> locations;
//
//	private double[][] pcaData;
//	
//	
//	public RegTreeNode(int index, Regtree tree, ParamConfigurationSpace configSpace, List<ProblemInstance> instances, ParamConfiguration incumbent, double[][]  pcaData)
//	{
//		this.index = index;
//		this.tree = tree;
//		this.configSpace = configSpace;
//		this.instances = instances;
//		this.incumbent = incumbent;
//		this.pcaData = pcaData;
//		
//		
//		if(tree.preprocessed)
//		{
//			
//			double[][] Theta = { incumbent.toValueArray() };
//			locations = RegtreeFwd.marginalFwdNodes(tree, Theta, null);
//			
//			
//			
//		} else
//		{
//			
//			double[][] Theta = { incumbent.toValueArray() };
//			double[] x_row = new double[incumbent.toValueArray().length + pcaData[0].length];
//			
//			double[] valueArray = incumbent.toValueArray();
//			locations = new HashSet<Integer>();
//			int j=0;
//			for(ProblemInstance instance : instances)
//			{
//				for(int i=0; i < valueArray.length; i++)
//				{
//					x_row[i] = valueArray[i];
//				}
//				
//				for(int i=0; i < pcaData[j].length; i++)
//				{
//					x_row[i + valueArray.length] = pcaData[j][i]; 
//				}
//				j++;
//				double[][] X = { x_row } ;
//				
//				System.out.println(Arrays.deepToString(X));
//				int[] nodes = RegtreeFwd.fwd(tree, X);
//				
//				System.out.println(instances.size());
//				
//				System.out.println(Arrays.toString(nodes));
//				for(int child : nodes)
//				{
//					locations.add(child);
//					System.out.println("Incumbent added to: " + child);
//				}
//				
//				
//			}
//			
//			
//			
//			
//			
//			
//		}
//		
//		//Set<Integer> locations = RegtreeFwd.marginalFwdNodes(tree, Theta, null);
//	}
//	
//	public RegTreeNode(int index, Regtree tree, ParamConfigurationSpace configSpace, List<ProblemInstance> instances, ParamConfiguration incumbent, double[][]  pcaData, Set<Integer> locations )
//	{
//		this.index = index;
//		this.tree = tree;
//		this.configSpace = configSpace;
//		this.instances = instances;
//		this.incumbent = incumbent;
//		this.locations = locations;
//		
//		
//		//Set<Integer> locations = RegtreeFwd.marginalFwdNodes(tree, Theta, null);
//	}
//	
//	@Override
//	public DSTreeNode[] DSgetChildren() {
//		
//		List<DSTreeNode> list = new ArrayList<DSTreeNode>(2); 
//		
//		for(int child : tree.children[index])
//		{
//			if(child == 0) continue;
//			list.add(new RegTreeNode(child, tree, configSpace, instances, incumbent,pcaData, locations));
//		}
//		
//		return list.toArray(new DSTreeNode[0]);
//	}
//
//	@Override
//	public Color DSgetColor() {
//		
//		Color c;
//		if(tree.var[index] < 0)
//		{
//			//Categorical
//			
//			c= Color.RED;
//			
//			
//		} else if(tree.var[index] == 0)
//		{
//			double[][] Theta = { incumbent.toValueArray() };
//			
//			if(locations.contains(index))
//			{
//				c = new Color(0x339933);
//			} else
//			{
//				c = Color.BLACK;
//			}
//		
//		} else
//		{
//			if(configSpace.getEmptyConfiguration().toValueArray().length < Math.abs(tree.var[index]))
//			{
//				c = Color.BLUE;
//			} else
//			{
//				c = Color.MAGENTA;
//			}
//			
//			
//		}
//		return c;
//	}
//
//	@Override
//	public Object DSgetValue() {
//
//		//StringBuilder sb = new StringBuilder();
//		StringWriter writer = new StringWriter();
//		PrintWriter pWriter = new PrintWriter(writer);
//		
//		pWriter.format("Node: %2d %n", index);
//		
//		
//		
//			pWriter.format("Var: %3d %n", tree.var[index]);
//		
//			
//			
//			
//			if(tree.var[index] != 0)
//			{
//				String varName ="???";
//				
//				if(tree.var[index] < configSpace.getCategoricalSize().length)
//				{
//					varName = configSpace.getParameterNames().get(Math.abs(tree.var[index]));
//				} else
//				{
//					
//					varName = "PCA" +  (tree.var[index] - configSpace.getCategoricalSize().length);
//					/*String key;
//					Iterator<String> it = instances.get(0).getFeatures().keySet().iterator();
//					
//					for(int i=0; i < instances.get(0).getFeatures().keySet().size(); i++)
//					{
//						key = it.next();
//						if(i == (tree.var[index] - configSpace.getCategoricalSize().length))
//						{
//							varName = key;
//						}
//					}*/
//					
//				}
//			
//			
//			pWriter.format("VarName:%n %s %n", varName);
//			}
//			pWriter.format("NPred: %.3f %n",  tree.nodepred[index]);
//			pWriter.format("NVar: %.3f %n",  tree.nodevar[index]);
//			pWriter.format("Size: %d %n",tree.nodesize[index]);
//			if(tree.weights != null)
//			{
//				pWriter.format("Weights: %.4f %n", tree.weights[index]);
//				pWriter.format("WeightPred: %.4f %n", tree.weightedpred[index]);
//				
//				
//				if(locations.contains(index))
//				{
//					pWriter.format("[INC] %n");
//				}
//			
//			} else
//			{
//				
//				if(locations.contains(index))
//				{
//					pWriter.format("[INC] %n");
//				}
//				
//
//			}
//		
//			
//		return writer.toString(); 
//	}
//
//}

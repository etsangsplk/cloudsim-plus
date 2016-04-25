package org.cloudbus.cloudsim.examples;

/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

import org.cloudbus.cloudsim.util.TableBuilderHelper;
import org.cloudbus.cloudsim.util.TextTableBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSimple;
import org.cloudbus.cloudsim.schedulers.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.resources.FileStorage;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSimple;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.listeners.EventListener;
import org.cloudbus.cloudsim.listeners.VmInsideHostEventInfo;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Ram;

/**
 * A simple example showing how to create a data center with 1 host and run 1 cloudlet on it,
 and receive notifications when a Host is allocated or deallocated to each VmSimple.
 * The example uses the new Vm listeners to gets these notifications
 while the simulation is running.
 * 
 * @see Vm#setOnHostAllocationListener(org.cloudbus.cloudsim.listeners.EventListener) 
 * @see Vm#setOnHostDeallocationListener(org.cloudbus.cloudsim.listeners.EventListener) 
 * @see Vm#setOnVmCreationFailureListener(org.cloudbus.cloudsim.listeners.EventListener) 
 * @see EventListener
 * 
 * @author Manoel Campos da Silva Filho
 */
public class VmListenersExample {
    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList;

    /** The vmlist. */
    private static List<Vm> vmlist;

    /**
     * Creates main() to run this example.
     *
     * @param args the args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
            Log.printFormattedLine("Starting %s ...", VmListenersExample.class.getSimpleName());

            try {
                    // First step: Initialize the CloudSim package. It should be called before creating any entities.
                    int num_user = 1; // number of cloud users
                    Calendar calendar = Calendar.getInstance(); // Calendar whose fields have been initialized with the current date and time.
                    boolean trace_flag = false; // trace events

                    /* Comment Start - Dinesh Bhagwat 
                     * Initialize the CloudSim library. 
                     * init() invokes initCommonVariable() which in turn calls initialize() (all these 3 methods are defined in CloudSim.java).
                     * initialize() creates two collections - an ArrayList of SimEntity Objects (named entities which denote the simulation entities) and 
                     * a LinkedHashMap (named entitiesByName which denote the LinkedHashMap of the same simulation entities), with name of every SimEntity as the key.
                     * initialize() creates two queues - a Queue of SimEvents (future) and another Queue of SimEvents (deferred). 
                     * initialize() creates a HashMap of of Predicates (with integers as keys) - these predicates are used to select a particular event from the deferred queue. 
                     * initialize() sets the simulation clock to 0 and running (a boolean flag) to false.
                     * Once initialize() returns (note that we are in method initCommonVariable() now), a CloudSimShutDown (which is derived from SimEntity) instance is created 
                     * (with numuser as 1, its name as CloudSimShutDown, id as -1, and state as RUNNABLE). Then this new entity is added to the simulation 
                     * While being added to the simulation, its id changes to 0 (from the earlier -1). The two collections - entities and entitiesByName are updated with this SimEntity.
                     * the shutdownId (whose default value was -1) is 0    
                     * Once initCommonVariable() returns (note that we are in method init() now), a CloudInformationService (which is also derived from SimEntity) instance is created 
                     * (with its name as CloudInformatinService, id as -1, and state as RUNNABLE). Then this new entity is also added to the simulation. 
                     * While being added to the simulation, the id of the SimEntitiy is changed to 1 (which is the next id) from its earlier value of -1. 
                     * The two collections - entities and entitiesByName are updated with this SimEntity.
                     * the cisId(whose default value is -1) is 1
                     * Comment End - Dinesh Bhagwat 
                     */
                    CloudSim.init(num_user, calendar, trace_flag);

                    // Second step: Create Datacenters
                    // Datacenters are the resource providers in CloudSim. We need at
                    // list one of them to run a CloudSim simulation
                    Datacenter datacenter0 = createDatacenter("Datacenter_0");

                    // Third step: Create Broker
                    DatacenterBroker broker = createBroker();
                    int brokerId = broker.getId();

                    // Fourth step: Create one virtual machine
                    vmlist = new ArrayList<>();

                    // VM description
                    int vmid = 0;
                    int mips = 1000;
                    long size = 10000; // image size (MB)
                    int ram = 512; // vm memory (MB)
                    long bw = 1000;
                    int pesNumber = 1; // number of cpus
                    String vmm = "Xen"; // VMM name

                    // create VM
                    Vm vm = new VmSimple(
                            vmid, brokerId, mips, pesNumber, ram, bw, size, 
                            vmm, new CloudletSchedulerTimeShared());
                    
                    // set the listeners to intercept allocation and deallocation of a Host to the VmSimple
                    vm.setOnHostAllocationListener(new EventListener<VmInsideHostEventInfo>() {
                        @Override
                        public void update(VmInsideHostEventInfo evt) {
                            Log.printFormattedLine(
                                    "\n\t#EventListener: Host %d allocated to Vm %d at time %.2f\n", 
                                    evt.getHost().getId(), evt.getVm().getId(), evt.getTime());
                        }
                    });

                    vm.setOnHostDeallocationListener(new EventListener<VmInsideHostEventInfo>() {
                        @Override
                        public void update(VmInsideHostEventInfo evt) {
                            Log.printFormattedLine(
                                    "\n\t#EventListener: Vm %d moved/removed from Host %d at time %.2f\n", 
                                    evt.getVm().getId(), evt.getHost().getId(), evt.getTime());
                        }
                    });

                    // add the VM to the vmList
                    vmlist.add(vm);

                    // submit vm list to the broker
                    broker.submitVmList(vmlist);

                    // Fifth step: Create one Cloudlet
                    cloudletList = new ArrayList<>();

                    // Cloudlet properties
                    int id = 0;
                    long length = 400000;
                    long fileSize = 300;
                    long outputSize = 300;
                    UtilizationModel utilizationModel = new UtilizationModelFull();

                    Cloudlet cloudlet = 
                            new CloudletSimple(id, length, pesNumber, fileSize, 
                                    outputSize, utilizationModel, utilizationModel, 
                                    utilizationModel);
                    cloudlet.setUserId(brokerId);
                    cloudlet.setVmId(vmid);

                    // add the cloudlet to the list
                    cloudletList.add(cloudlet);

                    // submit cloudlet list to the broker
                    broker.submitCloudletList(cloudletList);

                    // Sixth step: Starts the simulation
                    CloudSim.startSimulation();

                    CloudSim.stopSimulation();

                    //Final step: Print results when simulation is over
                    List<Cloudlet> newList = broker.getCloudletReceivedList();
                    TableBuilderHelper.print(new TextTableBuilder(), newList);
                    Log.printFormattedLine("%s finished!", VmListenersExample.class.getSimpleName());
            } catch (Exception e) {
                    e.printStackTrace();
                    Log.printLine("Unwanted errors happen");
            }
    }

    /**
     * Creates the datacenter.
     *
     * @param name the name
     *
     * @return the datacenter
     */
    private static Datacenter createDatacenter(String name) {

            // Here are the steps needed to create a DatacenterSimple:
            // 1. We need to create a list to store
            // our machine
            List<Host> hostList = new ArrayList<>();

            // 2. A Machine contains one or more PEs or CPUs/Cores.
            // In this example, it will have only one core.
            List<Pe> peList = new ArrayList<>();

            int mips = 1000;

            // 3. Create PEs and add these into a list.
            peList.add(new PeSimple(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

            // 4. Create Host with its id and list of PEs and add them to the list
            // of machines
            int hostId = 0;
            int ram = 2048; // host memory (MB)
            long storage = 1000000; // host storage
            long bw = 10000;

            hostList.add(new HostSimple(
                            hostId,
                            new ResourceProvisionerSimple<>(new Ram(ram)),
                            new ResourceProvisionerSimple<>(new Bandwidth(bw)),
                            storage,
                            peList,
                            new VmSchedulerTimeShared(peList)
                    )
            ); // This is our machine

            // 5. Create a DatacenterCharacteristics object that stores the
            // properties of a data center: architecture, OS, list of
            // Machines, allocation policy: time- or space-shared, time zone
            // and its price (G$/Pe time unit).
            String arch = "x86"; // system architecture
            String os = "Linux"; // operating system
            String vmm = "Xen";
            double time_zone = 10.0; // time zone this resource located
            double cost = 3.0; // the cost of using processing in this resource
            double costPerMem = 0.05; // the cost of using memory in this resource
            double costPerStorage = 0.001; // the cost of using storage in this
                                                                            // resource
            double costPerBw = 0.0; // the cost of using bw in this resource
            LinkedList<FileStorage> storageList = new LinkedList<>(); // we are not adding SAN
                                                                                                    // devices by now

            DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple (
                            arch, os, vmm, hostList, time_zone, cost, costPerMem,
                            costPerStorage, costPerBw);

            // 6. Finally, we need to create a DatacenterSimple object.
            Datacenter datacenter = null;
            try {
                    datacenter = new DatacenterSimple(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
            } catch (Exception e) {
                    e.printStackTrace();
            }

            return datacenter;
    }

    // We strongly encourage users to develop their own broker policies, to
    // submit vms and cloudlets according
    // to the specific rules of the simulated scenario
    /**
     * Creates the broker.
     *
     * @return the datacenter broker
     */
    private static DatacenterBroker createBroker() {
            DatacenterBroker broker = null;
            try {
                    broker = new DatacenterBrokerSimple("Broker");
            } catch (Exception e) {
                    e.printStackTrace();
                    return null;
            }
            return broker;
    }

}
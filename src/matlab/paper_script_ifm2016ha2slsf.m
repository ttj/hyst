% convert all the benchmarks for iccps 2016 paper

% close all slsf models (caution, will ignore any changes):
close all;
bdclose('all');

opt_simulate = 0; % do simulation after translation?

path_prefix = ['..', filesep, '..', filesep, 'examples', filesep, 'ifm2016ha2slsf', filesep];

models = {...
% moved all failing models up front for faster debugging
%['buck_hysteresis', filesep, 'buck_hysteresis_nodcm'], % broken in
%network, constant parsing changed, that issue is fixed, but now problem is
%related to network translation
%
%['cruise_control', filesep, 'cruise_control'], % broken with network changes, problem seems to be related to translateAutomaton.m issues mentioned
%
%['five_dim_linear_switch', filesep, 'five_dim_linear_switch'],  % broken with network change
%
%['rod_reactor', filesep, 'rod_reactor'], % broken in network:
%Java exception occurred:
%com.verivital.hyst.ir.AutomatonValidationException: BaseComponent
%<root>: Variables with defined flows in mode 'rod_1' ([x, c1, c2])
%differ from mode 'shutdown' ([])
%
['yaw_damper', filesep, 'continuized'], % network broken:
%Java exception occurred:
%com.verivital.hyst.ir.AutomatonExportException: explicit
%time-driven transitions not currently supported
%
%['yaw_damper', filesep, 'periodic'] % network broken, same as continuized model:
%Error using createConfigFromSpaceEx (line 39)
%Java exception occurred:
%com.verivital.hyst.ir.AutomatonExportException: explicit
%time-driven transitions not currently supported
%
% with latest updates after all merges the following models seem to work
%
['buck_hysteresis', filesep, 'buck_hysteresis'],
['filtered_oscillator', filesep, 'filtered_oscillator_4'], % broken in network, bad
%expression: x <= 0 & y + 0.714286 * x >= 0, although this should be a fine expression...
['filtered_oscillator', filesep, 'filtered_oscillator_32'], % likewise, broken in network: x <= 0 & y + 0.714286 * x >= 0
%
['glycemic_control', filesep, 'glycemic_control_3'], % broken in network:
%Runtime error: Data read before write
%Model Name: glycemic_control_3
%Block Name: glycemic_control_3/glycemic_1
%Data object v_tl(#88 (0:0:0)) was read before being written to.
%
['spiking_neuron', filesep, 'spiking_neuron_2'], % broken in network:
%Java exception occurred:
%com.verivital.hyst.ir.AutomatonExportException: Error while
%parsing expression: v - 40 + 0.1 * u <= 0
%
%
% all the next models at least translate without runtime crash
%
['biology_1', filesep, 'biology_1'],
['biology_2', filesep, 'biology_2'],
['bouncing_ball', filesep, 'bouncing_ball'],
['brusselator', filesep, 'brusselator'],
['buckling_column', filesep, 'buckling_column'],
%
%['Chemical_Akzo', filesep, 'Chemical_Akzo'], % skip, very large
%
['coupled_VDPOL', filesep, 'coupledVanderPol'],
%
['E5', filesep, 'E5'],
['fischer', filesep, 'fischer_N2_flat_safe'],
['fischer', filesep, 'fischer_N2_flat_unsafe'], 
%['five_vehicle_platoon', filesep, 'five_vehicle_platoon'], % broken with network changes
['glycemic_control', filesep, 'glycemic_control_1'],
['glycemic_control', filesep, 'glycemic_control_2'],
['glycemic_control_polynomial', filesep, 'glycemic_control_poly1'],
['glycemic_control_polynomial', filesep, 'glycemic_control_poly2'],
['helicopter', filesep, 'helicopter'],
['Hires', filesep, 'Hires'],
['jet_engine', filesep, 'jet_engine'],
['lac_operon', filesep, 'lac_operon'],
['lorentz', filesep, 'lorentz'],
['lotka_volterra', filesep, 'lotka_volterra'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n2'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n4'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n6'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n8'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n10'],
['non_linear_transmission_line_circuits', filesep, 'circuits_n12'],
['OREGO', filesep, 'OREGO'],
['randgen', filesep, 'randgen'],
%
% ['ring_modulator', filesep, 'ring_modulator'], % skip, very large
%
['Rober', filesep, 'Rober'],
['roessler', filesep, 'roessler'],
['Small_Circuit', filesep, 'small_circuit'],
['spiking_neuron', filesep, 'spiking_neuron'],
['spring_pendulum', filesep, 'spring_pendulum'],
['ten_vehicle_platoon', filesep, 'ten_vehicle_platoon'],
['spring_pendulum', filesep, 'spring_pendulum'],
['ten_vehicle_platoon', filesep, 'ten_vehicle_platoon'],
['roessler', filesep, 'roessler'],
['three_vehicle_platoon', filesep, 'three_vehicle_platoon'],
['two_tanks', filesep, 'two_tanks'],
['VDPOL', filesep, 'vanderpol'],
%
};

table_header = ['Name & Type & |Var| & |Loc| & |Trans| & t_{c} & t_{s} \tabularnewline ', sprintf('\n')];
table_string = table_header;

for i = 1 : length(models)
    tic; % reset timer for benchmarking
    
	name = models(i);
	name_str = char(name);
	[pathstr,filename,ext] = fileparts(name_str);
    
    xml_file = [path_prefix, pathstr, filesep, filename, '.xml'];
    cfg_file = [path_prefix, pathstr, filesep, filename, '.cfg'];
	
	[out_slsf_model, out_slsf_model_path, out_config] = SpaceExToStateflow(xml_file, cfg_file, '-s');
	
	if out_config.root.constants.containsKey('tmax')
		tmax = out_config.root.constants.get('tmax');
	else
		tmax = 5; % default time units (seconds)
    end
    
    conversionTime = toc;
	
	figure;
	hold on;
    
    tic; % reset timer
    if opt_simulate
        simulationLoop(filename, 2, tmax, 1000);
    end
    simulationTime = toc;
    
    % latex table string
    table_row = [strrep(filename, '_', '\_'), ...
        ' & ', 'todo type', ...
        ' & ', num2str(out_config.root.variables.size), ...
        ' & ', num2str(out_config.root.modes.size), ...
        ' & ', num2str(out_config.root.transitions.size), ...
        ' & ', num2str(conversionTime), ...
        ' & ', num2str(simulationTime), '\tabularnewline  ', sprintf('\n')];
    
    table_string = [table_string, table_row];
    
    close all; % kill figure
    
    %break; % stop after 1 for testing
end

table_string
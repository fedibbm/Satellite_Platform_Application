import { useState, useEffect, useCallback } from 'react';
import { analysisService, AnalysisResult, UpdateAnalysisData } from '@/services/analysis.service'; // Import UpdateAnalysisData

export function useProjectAnalysis(projectId: string | undefined | null, isActiveTab: boolean) {
    const [analysisResults, setAnalysisResults] = useState<AnalysisResult[]>([]);
    const [isLoadingAnalysis, setIsLoadingAnalysis] = useState(false);
    const [analysisError, setAnalysisError] = useState<string | null>(null);

    // State for deleting a result
    const [isDeletingAnalysis, setIsDeletingAnalysis] = useState<string | null>(null); // Store ID being deleted
    const [deleteAnalysisError, setDeleteAnalysisError] = useState<string | null>(null);
    const [deleteAnalysisSuccess, setDeleteAnalysisSuccess] = useState<string | null>(null);

    // State for updating a result
    const [editingResult, setEditingResult] = useState<AnalysisResult | null>(null); // Store the full result being edited
    const [isUpdatingAnalysis, setIsUpdatingAnalysis] = useState(false); // Changed to boolean
    const [updateAnalysisError, setUpdateAnalysisError] = useState<string | null>(null);
    const [updateAnalysisSuccess, setUpdateAnalysisSuccess] = useState<string | null>(null);


    // Function to fetch all analysis results for the project
    const fetchAnalysisResults = useCallback(async () => {
        if (!projectId) {
            setAnalysisResults([]);
            return;
        };

        setIsLoadingAnalysis(true);
        setAnalysisError(null);
        setDeleteAnalysisError(null); // Clear delete errors on refetch
        setDeleteAnalysisSuccess(null); // Clear delete success on refetch
        setUpdateAnalysisError(null); // Clear update errors on refetch
        setUpdateAnalysisSuccess(null); // Clear update success on refetch
        try {
            const results = await analysisService.getAnalysisResults(projectId);
            setAnalysisResults(results || []); // Ensure results is an array
        } catch (error: any) {
            console.error('Error fetching analysis results:', error);
            setAnalysisError(error.message || 'Failed to fetch analysis results');
            setAnalysisResults([]); // Clear results on error
        } finally {
            setIsLoadingAnalysis(false);
        }
    }, [projectId]);

    // Effect to fetch analysis results when the tab is active and projectId is valid
    useEffect(() => {
        if (isActiveTab && projectId) {
            fetchAnalysisResults();
        }
        // Clear results if tab becomes inactive or projectId is lost
        if (!isActiveTab || !projectId) {
            setAnalysisResults([]);
            setAnalysisError(null); // Clear error when not active
        }
    }, [projectId, isActiveTab, fetchAnalysisResults]);

    // Effect to load and auto-save the latest result from sessionStorage on mount
    useEffect(() => {
        const storedResult = sessionStorage.getItem('latestAnalysisResult');
        if (storedResult) {
            try {
                const parsedResult: AnalysisResult = JSON.parse(storedResult);
                sessionStorage.removeItem('latestAnalysisResult'); // Remove after reading

                // Since results are now auto-saved in the analysis page,
                // we just need to refresh the results list to show the newly saved result
                if (parsedResult.projectId === projectId) {
                    // Trigger a refresh of analysis results to show the newly saved one
                    fetchAnalysisResults();
                }

            } catch (e) {
                console.error("Failed to parse analysis result from sessionStorage", e);
                sessionStorage.removeItem('latestAnalysisResult'); // Clear corrupted data
            }
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId]); // Run only when projectId is established

    // Function to delete an analysis result
    const handleDeleteAnalysis = async (imageId: string, resultId: string) => {
        if (!imageId || !resultId) {
            setDeleteAnalysisError("Missing image ID or result ID for deletion.");
            return;
        }

        setIsDeletingAnalysis(resultId); // Set which ID is being deleted
        setDeleteAnalysisError(null);
        setDeleteAnalysisSuccess(null);

        try {
            await analysisService.deleteAnalysisResult(imageId, resultId);
            setDeleteAnalysisSuccess(`Result ${resultId} deleted successfully. Refreshing list...`);
            await fetchAnalysisResults(); // Refetch results after deletion
        } catch (error: any) {
            console.error("Error deleting analysis result:", error);
            setDeleteAnalysisError(error.message || "Failed to delete analysis result.");
        } finally {
            setIsDeletingAnalysis(null); // Clear deleting state
        }
    };

    // Function to initiate editing
    const startEditingAnalysis = (result: AnalysisResult) => {
        setEditingResult(result);
        setUpdateAnalysisError(null); // Clear previous errors
        setUpdateAnalysisSuccess(null);
    };

    // Function to cancel editing
    const cancelEditingAnalysis = () => {
        setEditingResult(null);
        setUpdateAnalysisError(null);
        setUpdateAnalysisSuccess(null);
    };

    // Function to perform the update API call - expects the *modified* data part
    const handleUpdateAnalysis = async (modifiedData: Partial<UpdateAnalysisData['data']>) => {
        if (!editingResult || !editingResult.id || !editingResult.imageId) {
            setUpdateAnalysisError("No result selected for editing or critical IDs are missing.");
            return;
        }
        if (!modifiedData) {
             setUpdateAnalysisError("Missing update data.");
             return;
        }

        setIsUpdatingAnalysis(true);
        setUpdateAnalysisError(null);
        setUpdateAnalysisSuccess(null);

        try {
            // Construct the full payload required by the backend PUT endpoint
            // We only allow editing 'notes' for now, but need to send the full structure
            const fullUpdateData: UpdateAnalysisData = {
                imageId: editingResult.imageId, // Required by backend
                data: {
                    // Include existing data, potentially overridden by modifiedData
                    indexType: editingResult.index_type,
                    meanValue: editingResult.statistics?.mean,
                    minValue: editingResult.statistics?.min,
                    maxValue: editingResult.statistics?.max,
                    processingTimeMs: editingResult.processing_duration * 1000,
                    ...modifiedData, // Apply changes (e.g., notes)
                },
                // Use existing date/type/status unless they are also editable
                date: editingResult.end_time, // Assuming end_time maps to the 'date' field
                type: 'VEGETATION_INDEX', // Assuming this is constant for now
                status: 'COMPLETED', // Assuming this is constant for now
            };

            await analysisService.updateAnalysisResult(editingResult.id, fullUpdateData);
            setUpdateAnalysisSuccess(`Result ${editingResult.id} updated successfully. Refreshing list...`);
            await fetchAnalysisResults(); // Refetch results after update
            cancelEditingAnalysis(); // Close modal on success
        } catch (error: any) {
            console.error("Error updating analysis result:", error);
            // Keep modal open on error to show message
            setUpdateAnalysisError(error.message || "Failed to update analysis result.");
        } finally {
            setIsUpdatingAnalysis(false); // Set to false when finished
        }
    };


    return {
        // State for list of results
        analysisResults,
        isLoadingAnalysis,
        analysisError,

        // State for deleting action
        isDeletingAnalysis,
        deleteAnalysisError,
        deleteAnalysisSuccess,

        // State for updating action
        editingResult, // Expose the result being edited for the modal
        isUpdatingAnalysis,
        updateAnalysisError,
        updateAnalysisSuccess,

        // Handlers
        fetchAnalysisResults, // Expose refetch function
        handleDeleteAnalysis, // Add delete handler
        startEditingAnalysis, // Add handler to start editing
        cancelEditingAnalysis,// Add handler to cancel editing
        handleUpdateAnalysis, // Update handler now takes partial data
    };
}

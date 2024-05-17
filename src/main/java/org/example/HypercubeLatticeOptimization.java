package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HypercubeLatticeOptimization {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            File jsonFile = new File("data.json"); // Path to your JSON file
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            Map<String, JsonNode> allViewsMap = createNodeMap(rootNode);
            int iterations = allViewsMap.size() / 2;
            List<String> materializedViews = greedySelectViews(allViewsMap, iterations);
            System.out.println("Selected views to materialize:");
            for (String viewName : materializedViews) {
                System.out.print(viewName);
                System.out.print("  ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, JsonNode> createNodeMap(JsonNode node) {
        Map<String, JsonNode> nodeMap = new HashMap<>();
        createNodeMapHelper(node, nodeMap);
        return nodeMap;
    }

    private static void createNodeMapHelper(JsonNode node, Map<String, JsonNode> nodeMap) {
        Set<String> childrenNames = new HashSet<>();
        calculateNumberOfChildren(node, childrenNames);
        int numChildren = childrenNames.size();
        ((ObjectNode) node).put("numChildren", numChildren);
        nodeMap.put(node.get("name").asText(), node);
        for (JsonNode child : node.get("children")) {
            createNodeMapHelper(child, nodeMap);
        }
    }

    private static void calculateNumberOfChildren(JsonNode node, Set<String> childrenNames) {
        for (JsonNode child : node.get("children")) {
            childrenNames.add(child.get("name").asText());
            calculateNumberOfChildren(child, childrenNames);
        }
    }

    private static List<String> greedySelectViews(Map<String, JsonNode> allViewsMap, int iterations) throws IOException {
        List<String> materializedViewsList = new ArrayList<>();
        Set<String> materializedViews = new HashSet<>();
        materializedViews.add("a"); // Assuming 'a' is the top view
        materializedViewsList.add("a");
        for (int i = 0; i < iterations; i++) {//Repeats iterations times
            System.out.println("Choice " + (i + 1));

            String bestView = null;
            int maxBenefit = Integer.MIN_VALUE;
            for (String viewName : allViewsMap.keySet()) {
                boolean currentViewIsNotMaterialized = !materializedViews.contains(viewName);
                JsonNode currentView = allViewsMap.get(viewName);
                if (currentViewIsNotMaterialized) {
                    int benefit = calculateBenefit(currentView, materializedViews, allViewsMap);
                    if (benefit > maxBenefit) {
                        maxBenefit = benefit;
                        bestView = viewName;
                    }
                }
            }

            if (bestView != null) {
                materializedViewsList.add(bestView);
                materializedViews.add(bestView);
                System.out.println("View to materialize: " + bestView);
            }
        }
        return materializedViewsList;
    }

    private static JsonNode getParentMaterializedView(JsonNode childView, Map<String, JsonNode> allViewsMap, Set<String> materializedViews) {
        JsonNode candidateParent = null;
        for (String materializedViewName : materializedViews) {
            JsonNode currentMaterializedView = allViewsMap.get(materializedViewName);
            if (isAncestor(currentMaterializedView, childView)) {
                if (candidateParent != null) {
                    int currentMaterializedViewWeight = currentMaterializedView.get("weight").asInt();
                    int candidateParentWeight = candidateParent.get("weight").asInt();
                    if (currentMaterializedViewWeight < candidateParentWeight) {
                        candidateParent = currentMaterializedView;
                    }
                } else {
                    candidateParent = currentMaterializedView;
                }
            }
        }
        return candidateParent;
    }

    private static int calculateBenefit(JsonNode currentView, Set<String> materializedViews, Map<String, JsonNode> allViewsMap) {
        int benefit = 0;
        String currentViewName = currentView.get("name").asText();

        int viewWeight = currentView.get("weight").asInt();
        JsonNode materializedParent = getParentMaterializedView(currentView, allViewsMap, materializedViews);
        int materializedParentViewWeight = materializedParent.get("weight").asInt();
        int parentBenefit = Math.max(materializedParentViewWeight - viewWeight, 0);

        Map<String, JsonNode> undisputedChildren = getUndisputedChildren(currentView, materializedViews, allViewsMap);
        int undisputedChildrenCount = undisputedChildren.size();

        int undisputedBenefit = parentBenefit * (undisputedChildrenCount + 1);

        Map<String, JsonNode> disputedChildren = getDisputedChildren(currentView, materializedViews, allViewsMap);

        int disputedBenefit = 0;
        for (JsonNode disputedChild : disputedChildren.values()) {
            JsonNode contestantParent = getParentMaterializedView(disputedChild, allViewsMap, materializedViews);
            int contestantParentWeight = contestantParent.get("weight").asInt();
            disputedBenefit = disputedBenefit + contestantParentWeight - viewWeight;

        }
        benefit = disputedBenefit + undisputedBenefit;
        String additonString = "";
        if (disputedBenefit > 0) {
            additonString = " + " + disputedBenefit;
        }
        String multiplicationString = "";
        if (undisputedChildrenCount > 0 || additonString.isEmpty()) {
            multiplicationString = " X " + (undisputedChildrenCount + 1);
        }
        System.out.println(currentViewName + ": " + parentBenefit + multiplicationString + additonString + " = " + benefit);

        return benefit;
    }

    private static Set<JsonNode> getAllChildren(JsonNode view) {
        Set<JsonNode> childrenSet = new HashSet<>();
        addChildren(view, childrenSet);
        return childrenSet;
    }

    private static void addChildren(JsonNode node, Set<JsonNode> childrenSet) {
        for (JsonNode child : node.get("children")) {
            childrenSet.add(child);
            // Recursively add all nested children
            addChildren(child, childrenSet);
        }
    }

    private static Map<String, JsonNode> getUndisputedChildren(JsonNode view, Set<String> materializedViews, Map<String, JsonNode> allViewsMap) {
        Map<String, JsonNode> undisputedChildren = new HashMap<>();

        for (JsonNode child : getAllChildren(view)) {
            if (isDisputed(child, view, materializedViews, allViewsMap).equals(Availability.available)) {
                undisputedChildren.put(child.get("name").asText(), child);
            }
        }

        return undisputedChildren;
    }

    private static Map<String, JsonNode> getDisputedChildren(JsonNode view, Set<String> materializedViews, Map<String, JsonNode> allViewsMap) {
        Map<String, JsonNode> disputedChildren = new HashMap<>();

        for (JsonNode child : getAllChildren(view)) {
            if (isDisputed(child, view, materializedViews, allViewsMap).equals(Availability.disputed)) {
                disputedChildren.put(child.get("name").asText(), child);
            }
        }

        return disputedChildren;
    }

    private static Availability isDisputed(JsonNode child, JsonNode currentView, Set<String> materializedViews, Map<String, JsonNode> allViewsMap) {
        JsonNode materializedParentOfChild = getParentMaterializedView(child, allViewsMap, materializedViews);
        int materializedParentViewWeight = materializedParentOfChild.get("weight").asInt();
        int viewWeight = currentView.get("weight").asInt();
        JsonNode materializedParentOfView = getParentMaterializedView(currentView, allViewsMap, materializedViews);

        String materializedParentOfViewName = materializedParentOfView.get("name").asText();
        String materializedParentOfChildName = materializedParentOfChild.get("name").asText();

        if (materializedParentViewWeight < viewWeight || materializedViews.contains(child.get("name").asText())) {
            return Availability.unavailable;
        }
        if (materializedParentOfChildName.equals(materializedParentOfViewName)) {
            return Availability.available;
        }
        if (materializedParentViewWeight > viewWeight) {
            return Availability.disputed;
        }

        return Availability.available;
    }

    private static boolean isAncestor(JsonNode ancestor, JsonNode descendant) {
        Set<JsonNode> childrenOfAncestor = getAllChildren(ancestor);
        String descendantName = descendant.get("name").asText();

        for (JsonNode child : childrenOfAncestor) {
            String childName = child.get("name").asText();
            if (childName.equals(descendantName)) {
                return true;
            }
        }
        return false;
    }

    enum Availability {
        available,
        disputed,
        unavailable
    }
}
